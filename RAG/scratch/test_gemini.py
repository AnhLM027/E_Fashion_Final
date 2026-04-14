import asyncio
import os
import sys

# Add app to path
sys.path.append("/app")

async def test_gemini():
    print("--- Gemini Diagnostic Script ---")
    try:
        from app.core.config import settings
        from app.services.llm import get_llm_provider
        from app.services.llm.types import LLMMessage
        
        print(f"Provider: {settings.LLM_PROVIDER}")
        print(f"Model: {settings.LLM_MODEL_FAST}")
        print(f"Key length: {len(settings.GOOGLE_AI_API_KEY) if settings.GOOGLE_AI_API_KEY else 0}")
        
        provider = get_llm_provider()
        
        test_msg = [LLMMessage(role="user", content="Xin chào, bạn có khỏe không? Trả lời ngắn gọn.")]
        
        print("Calling Gemini...")
        result = await provider.acomplete(messages=test_msg, think=False)
        print(f"Response: {result.content}")
        print("--- Success! ---")
    except Exception as e:
        import traceback
        print("--- FAILED ---")
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(test_gemini())
