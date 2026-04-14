import logging
import uuid
import json
from typing import Optional, Dict, Any, List
from pydantic import BaseModel
from fastapi import APIRouter, HTTPException

from app.services.rag_orchestrator import RAGOrchestrator
from app.services.chatbot_ami import ChatbotAmiService

logger = logging.getLogger(__name__)

router = APIRouter()

class DebateRoundRequest(BaseModel):
    user_id: str
    workspace_id: str
    session_id: Optional[str] = None
    subject_name: str
    student_answer: str
    history: List[Dict[str, str]] = []

@router.post("/round")
async def debate_round(req: DebateRoundRequest):
    try:
        session_id = req.session_id or str(uuid.uuid4())
        
        orchestrator = RAGOrchestrator(workspace_id=req.workspace_id)
        results = await orchestrator.retrieve_context(query=req.student_answer, top_k=3)
        context_str = results.context
        
        chatbot = ChatbotAmiService(mode="ami_debate_round")
        chat_result = await chatbot.chat(
            user_id=req.user_id,
            session_id=session_id,
            subject_name=req.subject_name,
            query=req.student_answer,
            context=context_str,
            history=req.history
        )
        
        return {
            "status": "success",
            "session_id": session_id,
            "ami_response": chat_result["response"]
        }
    except Exception as e:
        logger.error(f"Debate round error: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/score")
async def debate_score(req: DebateRoundRequest):
    try:
        orchestrator = RAGOrchestrator(workspace_id=req.workspace_id)
        # Bám sát vào nội dung mà sinh viên trả lời
        results = await orchestrator.retrieve_context(query=req.student_answer, top_k=5)
        context_str = results.context
        
        chatbot = ChatbotAmiService(mode="ami_debate_score")
        chat_result = await chatbot.chat(
            user_id=req.user_id,
            session_id=req.session_id or str(uuid.uuid4()),
            subject_name=req.subject_name,
            query=f"Hãy chấm điểm cho phần tranh biện sau đây của sinh viên:\n{req.student_answer}",
            context=context_str,
            history=req.history
        )
        
        response_text = chat_result["response"]
        
        # Vì mode là ami_debate_score, theo design prompt nó phải trả JSON
        try:
            score_data = json.loads(response_text)
        except json.JSONDecodeError:
            score_data = {"error": "Failed to parse LLM JSON", "raw": response_text}
            
        return {
            "status": "success",
            "evaluation": score_data
        }
    except Exception as e:
        logger.error(f"Debate score error: {e}")
        raise HTTPException(status_code=500, detail=str(e))
