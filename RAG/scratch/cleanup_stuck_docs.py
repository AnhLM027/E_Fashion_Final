import asyncio
from sqlalchemy import update
from app.core.database import AsyncSessionLocal
from app.models.document import Document, DocumentStatus

async def cleanup_stuck_docs():
    async with AsyncSessionLocal() as db:
        print("Cleaning up documents stuck in 'processing', 'parsing', or 'indexing'...")
        stmt = (
            update(Document)
            .where(Document.status.in_([DocumentStatus.PROCESSING, DocumentStatus.PARSING, DocumentStatus.INDEXING]))
            .values(status=DocumentStatus.FAILED, error_message="Reset by system administrator due to UI stuck.")
        )
        result = await db.execute(stmt)
        await db.commit()
        print(f"Cleanup done. {result.rowcount} documents reset to FAILED.")

if __name__ == "__main__":
    asyncio.run(cleanup_stuck_docs())
