import os
from sentence_transformers import SentenceTransformer, CrossEncoder

def download():
    models = {
        "embedding": "BAAI/bge-m3",
        "reranker": "BAAI/bge-reranker-v2-m3"
    }
    
    print("Pre-loading models...")
    print(f"Downloading embedding model: {models['embedding']}...")
    SentenceTransformer(models['embedding'])
    
    print(f"Downloading reranker model: {models['reranker']}...")
    CrossEncoder(models['reranker'])
    
    print("Models pre-loaded successfully!")

if __name__ == "__main__":
    download()
