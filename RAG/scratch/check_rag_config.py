from app.core.config import settings
from app.services.llm import get_llm_provider, get_embedding_provider

print(f"--- RAG CONFIGURATION CHECK ---")
print(f"Active Provider: {settings.LLM_PROVIDER}")
print(f"Main Model: {settings.LLM_MODEL}")
print(f"Fast Model: {settings.LLM_MODEL_FAST}")
print(f"Embedding Provider: {settings.KG_EMBEDDING_PROVIDER}")
print(f"Embedding Model: {settings.KG_EMBEDDING_MODEL}")

llm = get_llm_provider()
print(f"\nFinal LLM Instance Model: {getattr(llm, '_model', 'Unknown')}")

emb = get_embedding_provider()
print(f"Final Embedding Instance Model: {getattr(emb, '_model', 'Unknown')}")
