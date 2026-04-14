import asyncio
import os
import shutil
from pathlib import Path
from sqlalchemy import text
from app.core.database import AsyncSessionLocal
from app.core.config import settings

async def super_clear():
    # 1. Clear PostgreSQL
    try:
        async with AsyncSessionLocal() as db:
            print("--- Clearing PostgreSQL ---")
            await db.execute(text("TRUNCATE TABLE document_tables CASCADE;"))
            await db.execute(text("TRUNCATE TABLE document_images CASCADE;"))
            await db.execute(text("TRUNCATE TABLE documents CASCADE;"))
            await db.execute(text("TRUNCATE TABLE knowledge_bases CASCADE;"))
            await db.commit()
            print("PostgreSQL tables truncated.")
    except Exception as e:
        print(f"PostgreSQL clear failed: {e}")

    # 2. Clear Qdrant
    try:
        from app.db.qdrant import get_qdrant_client
        client = get_qdrant_client()
        collections = client.get_collections().collections
        print(f"--- Clearing Qdrant ({len(collections)} collections) ---")
        for col in collections:
            client.delete_collection(col.name)
            print(f"Deleted collection: {col.name}")
    except Exception as e:
        print(f"Qdrant clear failed: {e}")

    # 3. Clear Files
    dirs_to_wipe = [
        settings.BASE_DIR / "data" / "lightrag",
        settings.BASE_DIR / "data" / "docling",
        Path(settings.DATA_ROOT) / "data"
    ]
    print("--- Clearing Physical Files ---")
    for d in dirs_to_wipe:
        if d.exists():
            # Keep the directory but delete contents
            for item in d.iterdir():
                if item.is_dir():
                    shutil.rmtree(item)
                else:
                    item.unlink()
            print(f"Wiped directory: {d}")

    # 4. Clear MongoDB
    try:
        import motor.motor_asyncio
        client = motor.motor_asyncio.AsyncIOMotorClient(settings.MONGO_URI)
        db = client[settings.MONGO_DB_NAME]
        print(f"--- Clearing MongoDB ({settings.MONGO_DB_NAME}) ---")
        collections = await db.list_collection_names()
        for col in collections:
            await db[col].delete_many({})
            print(f"Cleared MongoDB collection: {col}")
    except Exception as e:
        print(f"MongoDB clear failed: {e}")

    print("\n--- ALL DATABASES CLEARED SUCCESSFULLY ---")

if __name__ == "__main__":
    asyncio.run(super_clear())
