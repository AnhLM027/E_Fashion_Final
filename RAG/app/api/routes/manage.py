import logging
import os
import shutil
from pathlib import Path
from fastapi import APIRouter, HTTPException
from app.services.vector_store import get_vector_store
from app.db.qdrant import qdrant_health_check
from sqlalchemy import select, delete, update
from app.core.database import AsyncSessionLocal
from app.models.document import Document
from app.models.knowledge_base import KnowledgeBase

logger = logging.getLogger(__name__)

from app.services.embedder import get_embedding_service
import uuid as uuid_lib

router = APIRouter()

@router.post("/update-qdrant")
async def update_qdrant(data: dict):
    """
    Cập nhật dữ liệu trực tiếp trong Qdrant (insert/update/delete).
    Tương đương với update-chroma cũ nhưng dùng Qdrant.
    """
    workspace_id = data.get("file_id") or data.get("workspace_id")
    action = data.get("action")
    
    if not workspace_id:
        raise HTTPException(status_code=400, detail="Missing workspace_id (file_id)")
        
    vs = get_vector_store(workspace_id)
    embedder = get_embedding_service()
    
    if action == "insert":
        insert_data = data.get("insert_data", [])
        if not insert_data:
            return {"status": "success", "message": "No data to insert"}
            
        texts = [item["text"] for item in insert_data]
        metadatas = [item.get("metadata", {}) for item in insert_data]
        ids = [str(uuid_lib.uuid4()) for _ in texts]
        
        # Add common metadata
        for meta in metadatas:
            meta["workspace_id"] = workspace_id
            meta["is_manual_contribution"] = True
            
        embeddings = embedder.embed_texts(texts)
        vs.add_documents(ids=ids, embeddings=embeddings, documents=texts, metadatas=metadatas)
        
        return {"status": "success", "message": f"Inserted {len(texts)} segments", "ids": ids}
        
    elif action == "update":
        doc_ids = data.get("document_ids", [])
        update_data = data.get("update_data", {})
        new_text = update_data.get("text")
        
        if not doc_ids or not new_text:
            raise HTTPException(status_code=400, detail="Missing document_ids or new text for update")
            
        # Qdrant update is essentially an upsert with the same ID.
        # We need to get the old metadata first to preserve it.
        # Note: VectorStore.get_by_ids returns {documents: [[]], metadatas: [[]]} which is a bit weird but we follow it.
        # wait, let's just re-embed and upsert.
        
        embeddings = embedder.embed_texts([new_text])
        for d_id in doc_ids:
            # Get existing metadata if possible
            existing = vs.get_by_ids([d_id])
            meta = existing["metadatas"][0][0] if existing["metadatas"] and existing["metadatas"][0] else {}
            
            vs.add_documents(
                ids=[d_id],
                embeddings=embeddings,
                documents=[new_text],
                metadatas=[meta]
            )
            
        return {"status": "success", "message": f"Updated {len(doc_ids)} segments"}
        
    elif action == "delete":
        doc_ids = data.get("document_ids", [])
        if not doc_ids:
            raise HTTPException(status_code=400, detail="Missing document_ids for delete")
            
        # We need a delete_by_ids in VectorStore. 
        # Current VectorStore only has delete_by_document_id (DB id) not point ID.
        # Let's use qdrant client directly or add to VectorStore.
        from app.db.qdrant import get_qdrant_client
        from qdrant_client.models import PointIdsList
        
        client = get_qdrant_client()
        client.delete(
            collection_name=vs.collection_name,
            points_selector=PointIdsList(points=doc_ids)
        )
        return {"status": "success", "message": f"Deleted {len(doc_ids)} segments"}
        
    else:
        raise HTTPException(status_code=400, detail=f"Unsupported action: {action}")

@router.get("/health")
async def health():
    """Kiểm tra sức khỏe Qdrant và ứng dụng."""
    status = qdrant_health_check()
    return {
        "status": "ok" if "offline" not in status else "error",
        "qdrant": status
    }

@router.get("/stats/{workspace_id}")
async def get_stats(workspace_id: str):
    """Lấy số lượng chunk đang có trong workspace."""
    try:
        vs = get_vector_store(workspace_id)
        count = vs.count()
        return {"workspace_id": workspace_id, "total_chunks": count}
    except Exception as e:
        raise HTTPException(status_code=404, detail="Workspace collection not found or error")

@router.delete("/{workspace_id}")
async def delete_workspace(workspace_id: str):
    """Xóa toàn bộ dữ liệu của một workspace (Vector, Files, KG, DB)."""
    logger.info(f"[DELETE] Full cleanup for workspace={workspace_id}")
    
    # 1. Xóa khỏi Vector Store (Qdrant)
    try:
        vs = get_vector_store(workspace_id)
        vs.delete_collection()
    except Exception as e:
        logger.warning(f"Failed to delete Qdrant collection for {workspace_id}: {e}")

    # 2. Xóa dữ liệu LightRAG KG từ ổ đĩa
    try:
        from app.core.config import settings
        kg_dir = settings.BASE_DIR / "data" / "lightrag" / f"kb_{workspace_id}"
        if kg_dir.exists():
            shutil.rmtree(kg_dir)
            logger.info(f"Deleted KG directory: {kg_dir}")
    except Exception as e:
        logger.warning(f"Failed to delete KG directory for {workspace_id}: {e}")

    # 3. Xóa dữ liệu File Upload vật lý
    try:
        from app.core.config import settings
        data_base_path = settings.DATA_BASE_PATH
        upload_dir = data_base_path / workspace_id
        if upload_dir.exists():
            shutil.rmtree(upload_dir)
            logger.info(f"Deleted physical upload directory: {upload_dir}")
    except Exception as e:
        logger.warning(f"Failed to delete upload directory for {workspace_id}: {e}")

    # 4. Xóa dữ liệu Parser (Ảnh/Bảng trích xuất)
    try:
        from app.core.config import settings
        # Xóa cả thư mục docling và marker của workspace này
        for parser in ["docling", "marker"]:
            parser_dir = settings.BASE_DIR / "data" / parser / f"kb_{workspace_id}"
            if parser_dir.exists():
                shutil.rmtree(parser_dir)
                logger.info(f"Deleted parser directory: {parser_dir}")
    except Exception as e:
        logger.warning(f"Failed to delete parser directories for {workspace_id}: {e}")

    # 5. Xóa dữ liệu Chat History và Debate (MongoDB)
    try:
        from app.db.mongodb import delete_chat_history_by_source, delete_debate_results_by_source
        delete_chat_history_by_source(workspace_id)
        delete_debate_results_by_source(workspace_id)
    except Exception as e:
        logger.warning(f"Failed to delete MongoDB records for {workspace_id}: {e}")

    # 6. Xóa khỏi Database (PostgreSQL)
    try:
        async with AsyncSessionLocal() as db:
            # Xóa các document trước (nếu không có cascade tự động)
            await db.execute(delete(Document).where(Document.workspace_id == workspace_id))
            # Xóa knowledge base
            await db.execute(delete(KnowledgeBase).where(KnowledgeBase.id == workspace_id))
            await db.commit()
            logger.info(f"Deleted DB records for workspace {workspace_id}")
    except Exception as e:
        logger.error(f"DB deletion failed for {workspace_id}: {e}")
        # Không raise lỗi ở đây vì các bước trước đã thành công, nhưng nên log lại.

    return {"status": "success", "message": f"Full cleanup completed for workspace {workspace_id}"}

@router.post("/{workspace_id}/clear-kg")
async def clear_workspace_kg(workspace_id: str):
    """
    Xóa sạch dữ liệu Knowledge Graph (LightRAG) của một workspace.
    Giữ lại các file và vector search (Qdrant), chỉ xóa phần 'đồ thị tri thức'.
    """
    logger.info(f"[CLEANUP] Clearing KG data for workspace={workspace_id}")
    try:
        from app.core.config import settings
        kg_dir = settings.BASE_DIR / "data" / "lightrag" / f"kb_{workspace_id}"
        if kg_dir.exists():
            shutil.rmtree(kg_dir)
            logger.info(f"Deleted KG directory: {kg_dir}")
            return {"status": "success", "message": f"KG cleared for workspace {workspace_id}. System will rebuild it on next ingestion."}
        else:
            return {"status": "success", "message": "KG directory already empty."}
    except Exception as e:
        logger.error(f"Failed to clear KG for {workspace_id}: {e}")
        raise HTTPException(status_code=500, detail=f"Error clearing KG: {str(e)}")

@router.get("/files/{workspace_id}")
async def list_files_in_db(workspace_id: str):
    """Lấy danh sách các file và trạng thái của chúng từ Database."""
    async with AsyncSessionLocal() as db:
        result = await db.execute(select(Document).where(Document.workspace_id == workspace_id).order_by(Document.created_at.desc()))
        docs = result.scalars().all()
        
        files_info = []
        for doc in docs:
            files_info.append({
                "id": doc.id,
                "filename": doc.filename,
                "original_filename": doc.original_filename,
                "status": doc.status.value,
                "size_bytes": doc.file_size,
                "page_count": doc.page_count,
                "chunk_count": doc.chunk_count,
                "image_count": doc.image_count,
                "table_count": doc.table_count,
                "error_message": doc.error_message,
                "uploaded_at": doc.created_at.isoformat(),
                "updated_at": doc.updated_at.isoformat(),
                "file_type": doc.file_type,
                "markdown_content": doc.markdown_content,
            })
            
    return {
        "workspace_id": workspace_id,
        "files": files_info,
        "total": len(files_info)
    }

@router.get("/analytics/{workspace_id}")
async def get_workspace_analytics(workspace_id: str):
    """Thống kê tổng quan về pipeline và KG của một workspace."""
    from app.models.document import DocumentStatus, DocumentImage, DocumentTable
    from sqlalchemy import func
    
    async with AsyncSessionLocal() as db:
        # Document stats
        result = await db.execute(select(Document).where(Document.workspace_id == workspace_id))
        docs = result.scalars().all()

        total = len(docs)
        indexed = sum(1 for d in docs if d.status == DocumentStatus.INDEXED)
        failed = sum(1 for d in docs if d.status == DocumentStatus.FAILED)
        processing = sum(1 for d in docs if d.status in [DocumentStatus.PROCESSING, DocumentStatus.PARSING, DocumentStatus.INDEXING])

        total_chunks = sum(d.chunk_count or 0 for d in docs)
        total_pages = sum(d.page_count or 0 for d in docs)
        total_images = sum(d.image_count or 0 for d in docs)
        total_tables = sum(d.table_count or 0 for d in docs)

        doc_breakdown = []
        for d in docs:
            doc_breakdown.append({
                "document_id": d.id,
                "filename": d.original_filename,
                "status": d.status.value,
                "chunk_count": d.chunk_count or 0,
                "page_count": d.page_count or 0,
                "image_count": d.image_count or 0,
                "table_count": d.table_count or 0,
                "file_size": d.file_size or 0,
                "processing_time_ms": d.processing_time_ms,
                "error_message": d.error_message,
                "updated_at": d.updated_at.isoformat(),
                "markdown_content": d.markdown_content,
            })

    # KG Analytics (best-effort)
    kg_analytics = None
    try:
        from app.services.knowledge_graph_service import KnowledgeGraphService
        kg = KnowledgeGraphService(workspace_id=workspace_id)
        kg_analytics = await kg.get_analytics()
    except Exception as e:
        logger.warning(f"KG analytics unavailable for workspace {workspace_id}: {e}")

    return {
        "workspace_id": workspace_id,
        "stats": {
            "total_documents": total,
            "indexed_documents": indexed,
            "failed_documents": failed,
            "processing_documents": processing,
            "total_chunks": total_chunks,
            "total_pages": total_pages,
            "image_count": total_images,
            "table_count": total_tables,
        },
        "kg_analytics": kg_analytics,
        "document_breakdown": doc_breakdown,
    }


@router.get("/collections")
async def list_collections():
    """Liệt kê tất cả Knowledge Base collections (workspaces)."""
    async with AsyncSessionLocal() as db:
        result = await db.execute(select(KnowledgeBase))
        collections = result.scalars().all()
        
        return {
            "collections": [
                {
                    "id": kb.id,
                    "name": kb.name,
                    "description": kb.description,
                    "created_at": kb.created_at.isoformat(),
                    "updated_at": kb.updated_at.isoformat(),
                }
                for kb in collections
            ],
            "total": len(collections)
        }


@router.get("/workspace/{workspace_id}")
async def get_workspace_details(workspace_id: str):
    """Lấy thông tin chi tiết Workspace và cấu hình KG."""
    async with AsyncSessionLocal() as db:
        result = await db.execute(select(KnowledgeBase).where(KnowledgeBase.id == workspace_id))
        kb = result.scalar_one_or_none()
        
        if not kb:
            raise HTTPException(status_code=404, detail="Workspace not found")
            
        return {
            "id": kb.id,
            "name": kb.name,
            "description": kb.description,
            "system_prompt": kb.system_prompt,
            "kg_language": kb.kg_language,
            "kg_entity_types": kb.kg_entity_types,
            "created_at": kb.created_at.isoformat(),
            "updated_at": kb.updated_at.isoformat(),
        }

@router.patch("/workspace/{workspace_id}")
async def update_workspace_settings(workspace_id: str, data: dict):
    """Cập nhật cấu hình Workspace (Entity types, Language, etc.)."""
    allowed_fields = ["name", "description", "system_prompt", "kg_language", "kg_entity_types"]
    update_data = {k: v for k, v in data.items() if k in allowed_fields}
    
    if not update_data:
        raise HTTPException(status_code=400, detail="No valid fields to update")
        
    async with AsyncSessionLocal() as db:
        stmt = update(KnowledgeBase).where(KnowledgeBase.id == workspace_id).values(**update_data)
        await db.execute(stmt)
        await db.commit()
        
    return {"status": "success", "message": f"Updated settings for workspace {workspace_id}"}


@router.delete("/files/{workspace_id}/{filename}")

async def delete_file_from_db(workspace_id: str, filename: str):
    """Xóa file khỏi database và vector store."""
    logger.info(f"[DELETE] Received request to delete file='{filename}' in workspace='{workspace_id}'")
    async with AsyncSessionLocal() as db:
        # Tìm document để lấy ID phục vụ xóa vector
        result = await db.execute(
            select(Document)
            .where(Document.workspace_id == workspace_id)
            .where(Document.original_filename == filename)
        )
        doc = result.scalar_one_or_none()
        
        if not doc:
            return {"status": "error", "message": f"File '{filename}' not found in DB for workspace {workspace_id}"}
        
        doc_id = doc.id
        
        # 1. Xóa khỏi Vector Store (Qdrant)
        try:
            vs = get_vector_store(workspace_id)
            vs.delete_by_document_id(doc_id)
        except Exception as e:
            logger.warning(f"Failed to delete vectors for doc {doc_id}: {e}")

        # 2. Xóa khỏi Database (PostgreSQL) - cascades based on model config
        await db.execute(delete(Document).where(Document.id == doc_id))
        await db.commit()
        
    return {"status": "success", "message": f"Deleted file '{filename}' from database and vector store."}

@router.delete("/file/{document_id}")
async def delete_file_by_id(document_id: int):
    """Xóa file khỏi database và vector store bằng ID."""
    logger.info(f"[DELETE] Received request to delete document_id={document_id}")
    async with AsyncSessionLocal() as db:
        result = await db.execute(select(Document).where(Document.id == document_id))
        doc = result.scalar_one_or_none()
        
        if not doc:
            raise HTTPException(status_code=404, detail=f"Document ID {document_id} not found")
        
        workspace_id = doc.workspace_id
        filename = doc.original_filename
        
        # 1. Xóa file vật lý
        try:
            from app.core.config import settings
            file_path = os.path.join(settings.DATA_BASE_PATH, workspace_id, filename)
            if os.path.exists(file_path):
                os.remove(file_path)
                logger.info(f"[DELETE] Removed physical file: {file_path}")
        except Exception as e:
            logger.warning(f"Failed to delete physical file {filename}: {e}")

        # 2. Xóa khỏi Vector Store (Qdrant)
        try:
            vs = get_vector_store(workspace_id)
            vs.delete_by_document_id(document_id)
        except Exception as e:
            logger.warning(f"Failed to delete vectors for doc {document_id}: {e}")

        # 3. Xóa khỏi Database (PostgreSQL)
        await db.execute(delete(Document).where(Document.id == document_id))
        await db.commit()
        
    return {"status": "success", "message": f"Deleted document ID {document_id} successfully."}
