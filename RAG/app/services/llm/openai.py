import logging
from typing import AsyncGenerator, Optional

from openai import AsyncOpenAI, OpenAI

from app.services.llm.base import LLMProvider
from app.services.llm.thinking_utils import extract_thinking_and_clean_answer
from app.services.llm.types import LLMMessage, LLMResult, StreamChunk

logger = logging.getLogger(__name__)

class OpenAILLMProvider(LLMProvider):
    """OpenAI GPT text/multimodal generation."""

    def __init__(self, api_key: str, model: str = "gpt-4o-mini", base_url: Optional[str] = None):
        self._api_key = api_key
        self._model = model
        self.client = AsyncOpenAI(api_key=api_key, base_url=base_url)
        self.sync_client = OpenAI(api_key=api_key, base_url=base_url)

    def _to_openai_messages(self, messages: list[LLMMessage]) -> list[dict]:
        """Convert LLMMessage list to OpenAI-standard message dicts."""
        result = []
        for msg in messages:
            content = []
            if msg.content:
                content.append({"type": "text", "text": msg.content})
            for img in msg.images:
                # OpenAI expects base64 URL or public URL
                import base64
                b64_data = base64.b64encode(img.data).decode("utf-8")
                content.append({
                    "type": "image_url",
                    "image_url": {"url": f"data:{img.mime_type};base64,{b64_data}"}
                })
            result.append({"role": msg.role, "content": content if msg.images else msg.content})
        return result

    def complete(
        self,
        messages: list[LLMMessage],
        *,
        temperature: float = 0.0,
        max_tokens: int = 4096,
        system_prompt: Optional[str] = None,
        think: bool = False,
    ) -> str | LLMResult:
        openai_msgs = self._to_openai_messages(messages)
        if system_prompt:
            openai_msgs.insert(0, {"role": "system", "content": system_prompt})

        try:
            response = self.sync_client.chat.completions.create(
                model=self._model,
                messages=openai_msgs,
                temperature=temperature,
                max_tokens=max_tokens,
            )
            text = response.choices[0].message.content or ""
            if think:
                content, thinking = extract_thinking_and_clean_answer(text)
                return LLMResult(content=content, thinking=thinking)
            return text
        except Exception as e:
            logger.error(f"OpenAI complete failed: {e}")
            return LLMResult(content="") if think else ""

    async def acomplete(
        self,
        messages: list[LLMMessage],
        *,
        temperature: float = 0.0,
        max_tokens: int = 4096,
        system_prompt: Optional[str] = None,
        think: bool = False,
    ) -> str | LLMResult:
        openai_msgs = self._to_openai_messages(messages)
        if system_prompt:
            openai_msgs.insert(0, {"role": "system", "content": system_prompt})

        try:
            response = await self.client.chat.completions.create(
                model=self._model,
                messages=openai_msgs,
                temperature=temperature,
                max_tokens=max_tokens,
            )
            text = response.choices[0].message.content or ""
            if think:
                content, thinking = extract_thinking_and_clean_answer(text)
                return LLMResult(content=content, thinking=thinking)
            return text
        except Exception as e:
            logger.error(f"OpenAI acomplete failed: {e}")
            return LLMResult(content="") if think else ""

    async def astream(
        self,
        messages: list[LLMMessage],
        *,
        temperature: float = 0.0,
        max_tokens: int = 4096,
        system_prompt: Optional[str] = None,
        think: bool = False,
        tools: list | None = None,
    ) -> AsyncGenerator[StreamChunk, None]:
        openai_msgs = self._to_openai_messages(messages)
        if system_prompt:
            openai_msgs.insert(0, {"role": "system", "content": system_prompt})

        try:
            stream = await self.client.chat.completions.create(
                model=self._model,
                messages=openai_msgs,
                temperature=temperature,
                max_tokens=max_tokens,
                stream=True,
                tools=tools,
            )
            accumulated_text = ""
            async for chunk in stream:
                if not chunk.choices:
                    continue
                delta = chunk.choices[0].delta
                if delta.content:
                    accumulated_text += delta.content
                    # Emit text chunk as-is; will extract thinking later
                    yield StreamChunk(type="text", text=delta.content)
                if delta.tool_calls:
                    for tc in delta.tool_calls:
                        yield StreamChunk(
                            type="function_call",
                            function_call={
                                "name": tc.function.name,
                                "args": tc.function.arguments,
                            }
                        )
            
            # After streaming completes, extract and emit thinking separately if present
            if think and accumulated_text:
                cleaned, thinking = extract_thinking_and_clean_answer(accumulated_text)
                if thinking:
                    yield StreamChunk(type="thinking", text=thinking)
        except Exception as e:
            logger.error(f"OpenAI streaming failed: {e}")
            yield StreamChunk(type="text", text="")

    def supports_vision(self) -> bool:
        return "gpt-4o" in self._model or "vision" in self._model

    def supports_thinking(self) -> bool:
        return True

    def supports_native_tools(self) -> bool:
        return True
