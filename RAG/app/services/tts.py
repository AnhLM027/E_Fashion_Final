import logging
import httpx
from typing import Optional

logger = logging.getLogger(__name__)

class TTSService:
    """
    Service for Text-to-Speech using OmniVoice API (aispeech.ptit.edu.vn)
    """
    API_URL = "http://10.170.100.221:8005/v1/stream-voice-design"

    async def generate_speech(self, text: str, speed: float = 1.2) -> Optional[bytes]:
        """
        Gửi yêu cầu tới OmniVoice API và trả về bytes của file âm thanh.
        """
        payload = {
            "text": text,
            "num_step": 64,
            "speed": speed,
            "instruct": "female, young adult, moderate pitch"
        }

        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                response = await client.post(
                    self.API_URL,
                    json=payload,
                    headers={"accept": "application/json"}
                )
                
                if response.status_code == 200:
                    # API trả về binary data (wav/mp3)
                    return response.content
                else:
                    logger.error(f"TTS API error: {response.status_code} - {response.text}")
                    return None
        except Exception as e:
            logger.error(f"TTS Exception: {e}")
            return None

_tts_service = None

def get_tts_service() -> TTSService:
    global _tts_service
    if _tts_service is None:
        _tts_service = TTSService()
    return _tts_service
