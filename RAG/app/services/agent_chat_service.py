import logging
import json
import asyncio
import uuid
from typing import AsyncGenerator, List, Dict, Any, Optional

from app.core.config import settings
from app.services.llm import get_llm_provider
from app.services.llm.thinking_utils import extract_thinking_and_clean_answer, _THINK_TAG_RE
from app.services.llm.types import LLMMessage, StreamChunk
from app.services.rag_orchestrator import RAGOrchestrator

logger = logging.getLogger(__name__)

# Constants
MAX_AGENT_ITERATIONS = 3
SSE_HEARTBEAT_INTERVAL = 15

class AgentChatService:
    """
    Agentic Chat Service - Semi-Agentic SSE Streaming
    Based on NexusRAG architecture.
    """
    def __init__(self, mode: str = "default"):
        self.mode = mode
        self.llm_provider = get_llm_provider()
        self.refiner_model = settings.LLM_MODEL  # Dùng chung model chính để chuẩn hóa

    def get_gemini_tool(self):
        """Tool definition for Gemini native function calling."""
        from google.genai import types
        return types.Tool(function_declarations=[
            types.FunctionDeclaration(
                name="search_documents",
                description=(
                    "Search the knowledge base for relevant document sections. "
                    "Use this tool when you need to answer based on documents."
                ),
                parameters={
                    "type": "OBJECT",
                    "properties": {
                        "query": {
                            "type": "STRING",
                            "description": "A specific search query based on the user's question.",
                        },
                    },
                    "required": ["query"],
                },
            ),
        ])

    async def _execute_search(self, workspace_id: str, query: str) -> Dict[str, Any]:
        """Execute search using RAGOrchestrator."""
        logger.info(f"Searching for workspace {workspace_id} with query: {query}")
        orchestrator = RAGOrchestrator(workspace_id=workspace_id)
        results = await orchestrator.retrieve_context(query=query, top_k=10)
        
        sources = [
            {
                "index": i + 1,
                "source": c.source_file,
                "page": c.page_no,
                "content": c.content,
                "heading": " > ".join(c.heading_path)
            } for i, c in enumerate(results.chunks)
        ]
        
        return {
            "context": results.context,
            "sources": sources,
            "image_refs": [
                {"id": img.image_id, "caption": img.caption} 
                for img in results.image_refs
            ]
        }

    async def chat_stream(
        self,
        user_id: str,
        session_id: str,
        workspace_id: str,
        query: str,
        history: List[Dict[str, str]] = None,
        think_enabled: bool = False,
        search_enabled: bool = False
    ) -> AsyncGenerator[Dict[str, Any], None]:
        """
        Semi-agentic chat loop with SSE streaming.
        Yields events: status, thinking, token, sources, complete, error.
        """
        messages: List[LLMMessage] = []
        if history:
            for msg in history[-10:]:
                if msg.get("content") and msg["content"].strip():
                    messages.append(LLMMessage(role=msg["role"], content=msg["content"]))
        
        messages.append(LLMMessage(role="user", content=query))
        
        # Tool setup logic matching NexusRAG
        is_gemini = settings.LLM_PROVIDER.lower() == "gemini"
        
        # Chỉ kích hoạt công cụ tìm kiếm nếu người dùng bật tính năng 'search'
        # và model hỗ trợ (hoặc chúng ta ép buộc search trước - force_search)
        active_tools = None
        if search_enabled and is_gemini:
            active_tools = [self.get_gemini_tool()]
        
        # System Prompt logic matching NexusRAG behavior
        system_prompt = (
            "Bạn là chuyên gia tư vấn thời trang của cửa hàng 'Style'. "
            "Phong cách: Lịch sự, chuyên nghiệp, tinh tế và luôn đặt sự hài lòng của khách hàng lên hàng đầu. "
            "NHIỆM VỤ CỦA BẠN:\n"
            "1. Dựa trên tài liệu được cung cấp để tư vấn chi tiết về sản phẩm (chất liệu, màu sắc, size, giá).\n"
            "2. Nếu tài liệu không có thông tin cụ thể, hãy sử dụng kiến thức thời trang chuyên nghiệp để gợi ý và khuyên khách hàng nhắn tin trực tiếp cho shop để được hỗ trợ chính xác nhất.\n"
            "3. Luôn khuyến khích khách hàng phối đồ (mix & match) để tạo phong cách riêng.\n"
            "4. Câu trả lời cần ngắn gọn, xúc tích, sử dụng emoji phù hợp với lĩnh vực thời trang (👕, 👗, ✨).\n"
            "5. Tuyệt đối không trả lời các vấn đề ngoài lĩnh vực thời trang hoặc cửa hàng Style.\n"
            "6. LUÔN TRẢ LỜI BẰNG TIẾNG VIỆT trong mọi trường hợp."
        )

        if search_enabled:
            system_prompt += "\n\nSử dụng công cụ 'search_documents' để tra cứu tài liệu khi thông tin hiện tại không đủ. "

        yield {"event": "status", "data": {"step": "analyzing", "detail": "Đang phân tích..."}}

        accumulated_answer = ""
        thinking_text = ""
        final_sources = []
        search_query = query

        # Always retrieve workspace context so chatbot có thể trả lời dựa trên tài liệu đã load.
        yield {"event": "status", "data": {"step": "retrieving", "detail": "Đang lấy nội dung tài liệu để trả lời..."}}
        search_result = await self._execute_search(workspace_id, query)
        final_sources = search_result["sources"]
        yield {"event": "sources", "data": {"sources": final_sources}}

        if search_result["context"]:
            messages.append(LLMMessage(
                role="system",
                content=(
                    "Tài liệu tham khảo: \n" + search_result["context"] +
                    "\n\nHãy sử dụng nội dung này khi trả lời." 
                )
            ))

        for iteration in range(MAX_AGENT_ITERATIONS):
            function_calls = []
            
            async for chunk in self.llm_provider.astream(
                messages=messages,
                system_prompt=system_prompt,
                temperature=0.7,
                max_tokens=2000 if think_enabled else 1000,
                think=think_enabled,
                tools=active_tools
            ):
                if chunk.type == "thinking":
                    thinking_text += chunk.text
                    yield {"event": "thinking", "data": {"text": chunk.text}}
                elif chunk.type == "function_call":
                    function_calls.append(chunk.function_call)
                elif chunk.type == "text":
                    # Accumulate original text (may contain thinking blocks from OpenAI)
                    accumulated_answer += chunk.text
                    # But emit only clean text to UI
                    clean_chunk_text = _THINK_TAG_RE.sub("", chunk.text).strip()
                    if clean_chunk_text:
                        yield {"event": "token", "data": {"text": clean_chunk_text}}

            if function_calls:
                fc = function_calls[0]
                if fc["name"] == "search_documents":
                    search_query = fc["args"].get("query", query)
                    yield {"event": "status", "data": {"step": "retrieving", "detail": f"Đang tìm kiếm: {search_query}..."}}
                    
                    search_result = await self._execute_search(workspace_id, search_query)
                    final_sources = search_result["sources"]
                    
                    yield {"event": "sources", "data": {"sources": final_sources}}
                    
                    # Thêm kết quả vào hội thoại
                    from google.genai import types as gtypes
                    
                    # Lưu raw assistant message với function call
                    raw_content = getattr(self.llm_provider, "last_response_content", None)
                    messages.append(LLMMessage(
                        role="assistant",
                        content="",
                        _raw_provider_content=raw_content
                    ))
                    
                    # Thêm function response
                    func_resp_content = gtypes.Content(
                        role="user",
                        parts=[gtypes.Part.from_function_response(
                            name="search_documents",
                            response={"result": search_result["context"]}
                        )]
                    )
                    messages.append(LLMMessage(
                        role="user",
                        content="",
                        _raw_provider_content=func_resp_content
                    ))
                    
                    yield {"event": "status", "data": {"step": "generating", "detail": "Đang tổng hợp câu trả lời..."}}
                    continue # Sang vòng lặp tiếp theo để LLM tổng hợp
            
            # Không còn function call hoặc đã xong
            break

        # Always extract thinking blocks from accumulated answer if thinking mode enabled
        if think_enabled and accumulated_answer:
            cleaned_answer, extracted_thinking = extract_thinking_and_clean_answer(accumulated_answer)
            accumulated_answer = cleaned_answer
            if extracted_thinking:
                thinking_text = (thinking_text + "\n\n" + extracted_thinking) if thinking_text else extracted_thinking
                yield {"event": "thinking", "data": {"text": extracted_thinking}}

        # LUÔN CHUẨN HÓA CÂU TRẢ LỜI (Xóa tiếng Anh rác, sửa lỗi định dạng)
        if accumulated_answer.strip():
            yield {"event": "status", "data": {"step": "refining", "detail": "Đang chuẩn hóa câu trả lời cuối cùng..."}}
            try:
                refined_answer = await self._refine_answer(accumulated_answer)
                if refined_answer:
                    accumulated_answer = refined_answer
            except Exception as e:
                logger.error(f"Refinement failed: {e}")

        # Lưu vào MongoDB (Background task)
        try:
            from app.db.mongodb import save_chat_message
            save_chat_message(
                user_id=user_id,
                session_id=session_id,
                message=query,
                response=accumulated_answer,
                thinking=thinking_text,
                source=workspace_id,
                sources=final_sources if final_sources else None,
                search_query=search_query if search_enabled else ""
            )
        except Exception as e:
            logger.error(f"Cannot save chat history: {e}")

        yield {"event": "complete", "data": {"answer": accumulated_answer, "sources": final_sources}}

    async def _refine_answer(self, raw_answer: str) -> str:
        """Sử dụng LLM để dọn dẹp và chuẩn hóa câu trả lời cuối cùng."""
        refine_prompt = (
            "Bạn là một biên tập viên chuyên nghiệp. Nhiệm vụ của bạn là CHUẨN HÓA câu trả lời từ một trợ lý ảo.\n"
            "YÊU CẦU:\n"
            "1. XÓA BỎ hoàn toàn các đoạn văn bản tiếng Anh mô tả quy trình suy nghĩ (như 'Strategizing complete', 'Mental Sandbox', v.v.).\n"
            "2. Đảm bảo toàn bộ câu trả lời là TIẾNG VIỆT tự nhiên, lịch sự.\n"
            "3. Giữ nguyên các thông tin về sản phẩm, giá cả, và địa chỉ từ nội dung gốc.\n"
            "4. Định dạng lại markdown cho đẹp mắt (nếu cần).\n"
            "5. CHỈ TRẢ VỀ nội dung câu trả lời đã sạch sẽ, không thêm bớt lời dẫn của bạn.\n\n"
            f"--- NỘI DUNG GỐC ---\n{raw_answer}\n--- KẾT THÚC ---"
        )
        
        try:
            # Gọi LLM (Sử dụng complete vì đây là bước xử lý khối văn bản duy nhất)
            result = await self.llm_provider.acomplete(
                messages=[LLMMessage(role="user", content=refine_prompt)],
                temperature=0.3,
                max_tokens=2000
            )
            
            # Xử lý kết quả trả về
            if hasattr(result, "content"):
                return result.content or raw_answer
            return str(result) or raw_answer
        except Exception as e:
            logger.error(f"Error in _refine_answer: {e}")
            return raw_answer
