import os
from pymongo import MongoClient
from app.core.config import settings

def debug_db():
    print(f"MONGO_URI: {settings.MONGO_URI}")
    print(f"MONGO_DB_NAME: {settings.MONGO_DB_NAME}")
    
    client = MongoClient(settings.MONGO_URI)
    db = client[settings.MONGO_DB_NAME]
    col = db["chat_history"]
    
    total = col.count_documents({})
    print(f"Total docs in {settings.MONGO_DB_NAME}.chat_history: {total}")
    
    sources = col.distinct("source")
    print(f"Available sources: {sources}")
    
    if total > 0:
        sample = col.find_one()
        print(f"Sample source: '{sample.get('source')}'")

if __name__ == "__main__":
    debug_db()
