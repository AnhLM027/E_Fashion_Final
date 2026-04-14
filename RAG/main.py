from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.routes import upload, chat, debate, history, manage, graph
from app.core.config import settings
from app.core.logging_config import setup_logging

# Initialize Logging
setup_logging()

import logging
logger = logging.getLogger(__name__)

async def cleanup_stuck_documents():
    """Dọn dẹp các tài liệu bị kẹt trạng thái khi khởi động server."""
    from app.core.database import AsyncSessionLocal
    from app.models.document import Document, DocumentStatus
    from sqlalchemy import update
    
    try:
        async with AsyncSessionLocal() as db:
            stmt = (
                update(Document)
                .where(Document.status.in_([
                    DocumentStatus.PROCESSING, 
                    DocumentStatus.PARSING, 
                    DocumentStatus.INDEXING
                ]))
                .values(
                    status=DocumentStatus.FAILED, 
                    error_message="Hệ thống đã khởi động lại trong khi đang xử lý. Vui lòng nhấn 'Xử lý' lại."
                )
            )
            result = await db.execute(stmt)
            await db.commit()
            if result.rowcount > 0:
                logger.info(f"--- [STARTUP CLEANUP] Reset {result.rowcount} stuck documents to FAILED ---")
    except Exception as e:
        logger.error(f"--- [STARTUP CLEANUP] Failed: {e} ---")

async def run_database_migrations():
    """Chạy database migrations khi khởi động server."""
    import subprocess
    import sys
    import asyncio
    
    try:
        logger.info("--- [STARTUP MIGRATION] Running database migrations ---")
        # Run migrations using asyncio subprocess
        process = await asyncio.create_subprocess_exec(
            sys.executable, "-m", "alembic", "upgrade", "head",
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
            cwd="/app"
        )
        stdout, stderr = await process.communicate()
        
        if process.returncode == 0:
            logger.info("--- [STARTUP MIGRATION] Migrations completed successfully ---")
        else:
            logger.error(f"--- [STARTUP MIGRATION] Failed: {stderr.decode()}")
    except Exception as e:
        logger.error(f"--- [STARTUP MIGRATION] Exception: {e} ---")

app = FastAPI(
    title=settings.APP_NAME,
    version="1.0.0",
    description="Hybrid RAG System combining NexusRAG pipeline with MBA_API stability.",
    on_startup=[run_database_migrations, cleanup_stuck_documents],
    openapi_tags=[
        {"name": "Upload", "description": "Upload and ingestion endpoints (vector + graph)"},
        {"name": "Chat", "description": "RAG chat endpoints"},
        {"name": "Ami Debate", "description": "Academic debate features"},
    ]
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(upload.router, prefix="/api/v1/upload", tags=["Upload"])
app.include_router(chat.router, prefix="/api/v1/chat", tags=["Chat"])
app.include_router(debate.router, prefix="/api/v1/debate", tags=["Ami Debate"])
app.include_router(history.router, prefix="/api/v1/history", tags=["History"])
app.include_router(manage.router, prefix="/api/v1/manage", tags=["DB Management"])
app.include_router(graph.router, prefix="/api/v1/graph", tags=["Knowledge Graph"])

@app.get("/health")
def health_check():
    return {"status": "ok", "app": settings.APP_NAME}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=4560, reload=True)
