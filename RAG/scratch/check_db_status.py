import asyncio
from sqlalchemy import select
from app.core.database import AsyncSessionLocal
from app.models.document import Document

async def check_doc_statuses():
    async with AsyncSessionLocal() as db:
        result = await db.execute(select(Document))
        docs = result.scalars().all()
        
        print(f"--- DOCUMENT STATUS SUMMARY ---")
        stats = {}
        for doc in docs:
            stats[doc.status.value] = stats.get(doc.status.value, 0) + 1
            if doc.status.value in ["processing", "parsing", "indexing"]:
                print(f"STUCK DOC: ID={doc.id}, Name={doc.original_filename}, Status={doc.status.value}, Created={doc.created_at}")
        
        print(f"\nTotal counts: {stats}")

if __name__ == "__main__":
    asyncio.run(check_doc_statuses())
