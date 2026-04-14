import logging
import uuid
from typing import Optional, Dict, Any, List
from pydantic import BaseModel, Field
from fastapi import APIRouter, HTTPException

from app.services.rag_orchestrator import RAGOrchestrator
from app.services.chatbot_ami import ChatbotAmiService
from app.services.agent_chat_service import AgentChatService
from app.core.database import AsyncSessionLocal
from app.models.knowledge_base import KnowledgeBase
from sqlalchemy import select
from app.db.mongodb import get_chatbot_name

logger = logging.getLogger(__name__)

router = APIRouter()

class ExplanationItem(BaseModel):
    question: str
    userAnswer: str
    correctAnswer: str
    remainingAnswers: List[str]
    isCorrect: bool

class ExplanationRequest(BaseModel):
    workspace_id: str
    subject_name: str
    explanations: List[ExplanationItem]

class ChatRequest(BaseModel):
    user_id: str = Field(..., alias="userId")
    workspace_id: str = Field(..., alias="source")
    query: str = Field(..., alias="text")
    mode: str = "default"  # can be "ami_study" / "default"
    session_id: Optional[str] = Field(None, alias="sessionId")
    subject_name: Optional[str] = None
    history: List[Dict[str, str]] = []
    think: bool = False
    search: bool = False

    class Config:
        populate_by_name = True

@router.post("/")
async def chat_endpoint(req: ChatRequest):
    try:
        req.think = False # Tắt hoàn toàn thinking mode
        session_id = req.session_id or str(uuid.uuid4())

        # 1. Orchestrator: Retrieve and Rerank
        orchestrator = RAGOrchestrator(workspace_id=req.workspace_id)
        # retrieve_context returns DeepRetrievalResult
        results = await orchestrator.retrieve_context(query=req.query, top_k=5)
        
        context_str = results.context
        
        if not context_str or not context_str.strip():
            context_str = "Không tìm thấy nội dung phù hợp trong tài liệu."

        # 1.5 Fetch Subject Name from DB if not provided
        subj = req.subject_name
        async with AsyncSessionLocal() as db:
            kb_res = await db.execute(select(KnowledgeBase).where(KnowledgeBase.id == req.workspace_id))
            kb = kb_res.scalar_one_or_none()
            
            # Workspace name is used for subject_name context
            if not subj or subj == "Tri thức chung":
                subj = kb.name if kb else "Tri thức chung"
            
            # Fetch specific Chatbot Name from MongoDB (takes precedence for identity)
            cbot_name = get_chatbot_name(req.workspace_id)
            if not cbot_name:
                cbot_name = kb.name if kb else "Style"
                
            logger.info(f"[CHAT] Workspace={req.workspace_id}, Resolved Chatbot Name='{cbot_name}'")

        # 2. Chatbot Service
        chatbot = ChatbotAmiService(mode=req.mode)
        # Bổ sung tham số think
        chat_result = await chatbot.chat(
            user_id=req.user_id,
            session_id=session_id,
            subject_name=cbot_name,
            query=req.query,
            context=context_str,
            history=req.history,
            workspace_id=req.workspace_id,
            think=req.think
        )

        return {
            "status": "success",
            "session_id": session_id,
            "response": chat_result["response"],
            "thinking": chat_result["thinking"],
            "sources_count": len(results.chunks),
            "sources": [
                {
                    "source": c.source_file,
                    "page": c.page_no,
                    "heading": " > ".join(c.heading_path)
                } for c in results.chunks
            ],
            "images": [
                {
                    "id": img.image_id,
                    "caption": img.caption,
                    "url": f"/app/data/docling/kb_{req.workspace_id}/images/{img.image_id}.png" # Example path
                } for img in results.image_refs
            ],
            "tables": [
                {
                    "id": tbl.table_id,
                    "caption": tbl.caption,
                    "content": tbl.content_markdown
                } for tbl in results.table_refs
            ]
        }

    except Exception as e:
        logger.exception(f"Chat error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/explanation")
async def quiz_explanation(req: ExplanationRequest):
    """
    Tạo lời giải thích cho kết quả làm Quiz dựa trên tài liệu môn học.
    """
    try:
        orchestrator = RAGOrchestrator(workspace_id=req.workspace_id)
        chatbot = ChatbotAmiService(mode="ami_study")
        
        results = []
        for item in req.explanations:
            context_results = await orchestrator.retrieve_context(query=item.question, top_k=3)
            context_str = context_results.context
            
            prompt = f"""
            Câu hỏi: {item.question}
            Đáp án đúng: {item.correctAnswer}
            Câu trả lời của người học: {item.userAnswer}
            Các đáp án khác: {', '.join(item.remainingAnswers)}
            
            Dựa trên tài liệu môn học, hãy giải thích ngắn gọn tại sao '{item.correctAnswer}' là đáp án đúng và phân tích nhẹ nhàng các đáp án còn lại. 
            Kết quả của người học là: {'Đúng' if item.isCorrect else 'Chưa đúng'}.
            """
            
            chat_result = await chatbot.chat(
                user_id="quiz_system",
                session_id=str(uuid.uuid4()),
                subject_name=req.subject_name,
                query=prompt,
                context=context_str
            )
            explanation = chat_result["response"]
            
            results.append({
                "question": item.question,
                "is_correct": item.isCorrect,
                "explanation": explanation
            })
            
        return {"status": "success", "explanations": results}
        
    except Exception as e:
        logger.error(f"Explanation error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

class TTSRequest(BaseModel):
    text: str

@router.post("/tts")
async def text_to_speech(req: TTSRequest):
    """
    Chuyển đổi văn bản thành giọng nói.
    """
    from app.services.tts import get_tts_service
    from fastapi.responses import Response
    
    tts_service = get_tts_service()
    audio_content = await tts_service.generate_speech(req.text)
    
    if not audio_content:
        raise HTTPException(status_code=500, detail="Failed to generate speech")
        
    return Response(content=audio_content, media_type="audio/wav")

@router.post("/chat/ami")
async def chat_ami(req: ChatRequest):
    """
    Hội thoại mode-based (Ami study/debate) - Streaming.
    """
    req.think = False
    from fastapi.responses import StreamingResponse
    import json

    agent_service = AgentChatService(mode=req.mode)
    session_id = req.session_id or str(uuid.uuid4())

    async def event_generator():
        try:
            async for event in agent_service.chat_stream(
                user_id=req.user_id,
                session_id=session_id,
                workspace_id=req.workspace_id,
                query=req.query,
                history=req.history,
                think_enabled=req.think,
                search_enabled=req.search
            ):
                yield f"event: {event['event']}\ndata: {json.dumps(event['data'], ensure_ascii=False)}\n\n"
        except Exception as e:
            logger.error(f"Ami Stream error: {e}")
            yield f"event: error\ndata: {json.dumps({'message': str(e)}, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")

@router.post("/stream")
async def chat_stream_endpoint(req: ChatRequest):
    """
    Streaming Chat (SSE) - Agentic RAG
    """
    req.think = False
    from fastapi.responses import StreamingResponse
    import json

    agent_service = AgentChatService(mode=req.mode)
    session_id = req.session_id or str(uuid.uuid4())

    async def event_generator():
        try:
            async for event in agent_service.chat_stream(
                user_id=req.user_id,
                session_id=session_id,
                workspace_id=req.workspace_id,
                query=req.query,
                history=req.history,
                think_enabled=req.think,
                search_enabled=req.search
            ):
                # Format SSE
                yield f"event: {event['event']}\ndata: {json.dumps(event['data'], ensure_ascii=False)}\n\n"
        except Exception as e:
            logger.error(f"Stream error: {e}")
            yield f"event: error\ndata: {json.dumps({'message': str(e)}, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")
