import asyncio
import logging
from app.core.database import engine, Base
from app.models.knowledge_base import KnowledgeBase
from app.models.document import Document, DocumentImage, DocumentTable

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

async def create_tables():
    logger.info("Connecting to database to create tables...")
    async with engine.begin() as conn:
        # Import all models to ensure they are registered with Base
        await conn.run_sync(Base.metadata.create_all)
    logger.info("Tables created successfully.")
    await engine.dispose()

if __name__ == "__main__":
    asyncio.run(create_tables())
