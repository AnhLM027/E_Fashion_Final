from google import genai
import os
from dotenv import load_dotenv

load_dotenv("/home/naver/Desktop/AnhLM027/E_Fashion/RAG/.env")

api_key = os.getenv("GOOGLE_AI_API_KEY")
client = genai.Client(api_key=api_key)

print("Listing models...")
for model in client.models.list():
        print(f"Name: {model.name}, Supported Actions: {model.supported_actions}")
