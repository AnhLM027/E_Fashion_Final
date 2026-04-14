import logging
import json
from typing import List, Dict, Any, Optional
from app.core.config import settings
from app.db.mongodb import save_chat_message
from app.services.llm import get_llm_provider
from app.services.llm.thinking_utils import extract_thinking_and_clean_answer

logger = logging.getLogger(__name__)

# Khởi tạo LLM Provider từ cấu hình (Modular)
llm_provider = get_llm_provider()

class ChatbotAmiService:
    """
    Quản lý các Mode hội thoại của Ami: ami_study, ami_debate_round, ami_debate_score.
    """
    def __init__(self, mode: str = "default"):
        self.mode = mode

    def build_system_prompt(self, subject_name: str, context: str) -> str:
        base_context = f"\n\n--- CĂN CỨ TÀI LIỆU CỬA HÀNG {subject_name} ---\n{context}\n--- KẾT THÚC TÀI LIỆU ---"
        
        if self.mode == "ami_study" or self.mode == "default":
            return f"""Bạn đang hoạt động ở chế độ Chuyên gia tư vấn thời trang Style.
Danh tính:
- Bạn là chuyên gia tư vấn ảo của cửa hàng thời trang Style.
- Phong cách: Tinh tế, am hiểu xu hướng và luôn sẵn lòng giúp khách hàng tỏa sáng.
Mục tiêu:
- Giải đáp thắc mắc về sản phẩm, tư vấn chọn size, phối đồ và thông báo các chính sách ưu đãi của cửa hàng {subject_name}.
- PHẢI ưu tiên thông tin từ tài liệu. Nếu tài liệu thiếu, hãy tư vấn dựa trên kinh nghiệm thời trang nhưng khuyên khách hàng liên hệ trực tiếp để có thông tin chính xác nhất.
- BẮT BUỘC trả lời bằng tiếng Việt.{base_context}"""

        elif self.mode == "ami_debate_round":
            return f"""Bạn đang hoạt động ở chế độ hỗ trợ khách hàng chuyên sâu môn {subject_name}.
Danh tính:
- Nhân viên tư vấn Style sắc sảo, tinh tế.
Luật:
- BẮT BUỘC bám sát thông tin sản phẩm bên dưới.
- KHÔNG giải thích dài dòng. Tập trung vào việc đưa ra gợi ý phối đồ hoặc lựa chọn sản phẩm phù hợp.
- Tối đa 90 từ.{base_context}"""

        return f"Bạn là trợ lý ảo hỗ trợ khách hàng của cửa hàng {subject_name}.\n{base_context}"

    async def chat(
        self, 
        user_id: str, 
        session_id: str, 
        subject_name: str, 
        query: str, 
        context: str, 
        history: List[Dict[str, str]] = None,
        workspace_id: str = None,
        think: bool = False
    ) -> Dict[str, Any]:
        """Hội thoại với tùy chọn Thinking mode"""
        
        from app.services.llm.types import LLMMessage
        
        system_prompt = self.build_system_prompt(subject_name, context)
        if think:
            system_prompt += (
                "\n\nQUY TẮC SUY NGHĨ: Nếu bật think, trước khi trả lời hãy viết phần suy nghĩ nội bộ. "
                "Dùng khối `<think>...</think>` hoặc bắt đầu bằng `THINKING:` rồi mới đến phần trả lời."
            )
        
        # Build messages list starting only with history/user query
        messages = []
        
        if history:
            for msg in history:
                messages.append(LLMMessage(
                    role=msg.get("role", "user"), 
                    content=msg.get("content", "")
                ))
                
        messages.append(LLMMessage(role="user", content=query))

        logger.info(f"Sending request to LLM (think={think}) using provider: {settings.LLM_PROVIDER}")
        
        # Sử dụng acomplete với system_prompt riêng biệt
        result = await llm_provider.acomplete(
            messages=messages,
            system_prompt=system_prompt,
            temperature=0.3 if self.mode == "ami_debate_score" else 0.7,
            max_tokens=2000 if think else 800, # Tăng tokens nếu cần suy nghĩ
            think=think
        )

        response_text = ""
        thinking_text = ""

        if hasattr(result, "content"): # Đối tượng LLMResult
            response_text = result.content
            thinking_text = getattr(result, "thinking", "")
        else:
            response_text = str(result)

        if think and not thinking_text:
            cleaned_answer, extracted_thinking = extract_thinking_and_clean_answer(response_text)
            if extracted_thinking:
                thinking_text = extracted_thinking
                response_text = cleaned_answer

        # Lưu lịch sử sang MongoDB
        try:
            from app.db.mongodb import save_chat_message
            save_chat_message(
                user_id=user_id,
                session_id=session_id,
                message=query,
                response=response_text,
                thinking=thinking_text, # Lưu thêm phần tư duy
                source=workspace_id or subject_name
            )
        except Exception as e:
            logger.error(f"Cannot save chat history: {e}")

        return {
            "response": response_text,
            "thinking": thinking_text
        }
