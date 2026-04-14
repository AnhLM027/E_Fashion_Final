"""
MBA_RAG - File Upload & Ingestion Pipeline
===========================================
Module: app/api/routes/upload.py

Mô tả:
    Upload files và khởi động ingestion pipeline NexusRAG-style đã được tách biệt:
        1. Upload: Lưu file vào thư mục vật lý, tạo bản ghi DB (PENDING).
        2. Process: Chạy pipeline (Docling -> Dedup -> Embed -> Qdrant -> KG).

Routes:
    POST /         - Upload files và lưu trạng thái PENDING
    POST /process  - Kích hoạt xử lý cho các file cụ thể
"""

import logging
import asyncio
import os
import uuid as uuid_lib
from typing import List, Optional
from fastapi import APIRouter, UploadFile, File, Form, BackgroundTasks, HTTPException
from fastapi.responses import JSONResponse

import time
from sqlalchemy import select, update, text, delete
from app.core.config import settings
from app.core.database import AsyncSessionLocal
from app.models.document import Document, DocumentStatus, DocumentImage, DocumentTable
from app.models.knowledge_base import KnowledgeBase
from app.services.document_parser import get_document_parser
from app.services.chunker import get_chunker
from app.services.embedder import get_embedding_service
from app.services.vector_store import get_vector_store
from app.services.knowledge_graph_service import KnowledgeGraphService
from app.services.chunk_dedup import deduplicate_chunks

logger = logging.getLogger(__name__)

router = APIRouter()

DATA_BASE_PATH = settings.DATA_BASE_PATH

async def process_file_background(
    workspace_id: str,
    file_paths: List[str],
    file_names: List[str],
    document_ids: List[int],
):
    """
    Background ingestion pipeline (NexusRAG FULL Pipeline):
        SQL Init → Docling parse → Chunk Dedup → Embed → Qdrant index → KG ingest
    """
    import anyio
    try:
        logger.info(f"[INGEST] Starting for workspace={workspace_id}, files={file_names}")

        parser = get_document_parser(workspace_id)
        vector_store = get_vector_store(workspace_id)
        embedder = get_embedding_service()

        async with AsyncSessionLocal() as db:
            for file_path, file_name, doc_id in zip(file_paths, file_names, document_ids):
                start_time = time.time()
                all_enriched_chunks = []
                try:
                    # ── Stage 1: PARSING ───────────────────────────────────────
                    stmt = update(Document).where(Document.id == doc_id).values(status=DocumentStatus.PARSING)
                    await db.execute(stmt)
                    await db.commit()

                    # Cleanup old image/table records if re-processing
                    await db.execute(delete(DocumentImage).where(DocumentImage.document_id == doc_id))
                    await db.execute(delete(DocumentTable).where(DocumentTable.document_id == doc_id))
                    await db.commit()

                    # ── Step 2: Parse with Docling (Images/Tables included) ───────
                    parsed_doc = await anyio.to_thread.run_sync(
                        parser.parse,
                        file_path,
                        doc_id,
                        file_name
                    )

                    # ── Step 3: Save Images & Tables to DB ────────────────────────
                    for img in parsed_doc.images:
                        db_img = DocumentImage(
                            document_id=doc_id,
                            image_id=img.image_id,
                            page_no=img.page_no,
                            file_path=img.file_path,
                            caption=img.caption,
                            width=img.width,
                            height=img.height,
                            mime_type=img.mime_type
                        )
                        db.add(db_img)

                    for tbl in parsed_doc.tables:
                        db_tbl = DocumentTable(
                            document_id=doc_id,
                            table_id=tbl.table_id,
                            page_no=tbl.page_no,
                            content_markdown=tbl.content_markdown,
                            caption=tbl.caption,
                            num_rows=tbl.num_rows,
                            num_cols=tbl.num_cols
                        )
                        db.add(db_tbl)

                    stmt_update = (
                        update(Document)
                        .where(Document.id == doc_id)
                        .values(
                            markdown_content=parsed_doc.markdown,
                            page_count=parsed_doc.page_count,
                            image_count=len(parsed_doc.images),
                            table_count=len(parsed_doc.tables),
                            parser_version=parser.parser_name
                        )
                    )
                    await db.execute(stmt_update)
                    await db.commit()

                    all_enriched_chunks.extend(parsed_doc.chunks)
                    logger.info(f"[INGEST] Parsed '{file_name}': {len(parsed_doc.chunks)} enrichment chunks")

                except Exception as e:
                    logger.error(f"[INGEST] Failed to parse '{file_name}': {e}")
                    stmt = update(Document).where(Document.id == doc_id).values(status=DocumentStatus.FAILED, error_message=str(e))
                    await db.execute(stmt)
                    await db.commit()
                    continue

                if not all_enriched_chunks:
                    logger.warning(f"[INGEST] No chunks generated for {file_name}.")
                    stmt = update(Document).where(Document.id == doc_id).values(status=DocumentStatus.INDEXED)
                    await db.execute(stmt)
                    await db.commit()
                    continue

                # ── Step 4: Chunk Deduplication ───────────────────────────────────
                try:
                    deduped_chunks, dedup_stats = await anyio.to_thread.run_sync(
                        deduplicate_chunks, 
                        all_enriched_chunks
                    )
                    logger.info(f"[INGEST] Dedup for {file_name}: {dedup_stats['input']} → {dedup_stats['output']} chunks")
                except Exception as e:
                    logger.warning(f"[INGEST] Dedup failed: {e}")
                    deduped_chunks = all_enriched_chunks

                # ── Stage 2: INDEXING ────────────────────────────────────────
                stmt = update(Document).where(Document.id == doc_id).values(status=DocumentStatus.INDEXING)
                await db.execute(stmt)
                await db.commit()

                # Cleanup old chunks from Qdrant if re-processing
                await anyio.to_thread.run_sync(vector_store.delete_by_document_id, doc_id)

                # ── Step 5: Embed & Index into Qdrant ────────────────────────────
                texts = [c.content for c in deduped_chunks]
                embeddings = embedder.embed_texts(texts)

                # image_id -> URL lookup for metadata
                img_url_map = {
                    img.image_id: f"/static/doc-images/{workspace_id}/images/{img.image_id}.png"
                    for img in parsed_doc.images
                }

                ids = [str(uuid_lib.uuid4()) for _ in deduped_chunks]
                metadatas = []
                for chunk in deduped_chunks:
                    metadatas.append({
                        "file_name": chunk.source_file,
                        "document_id": chunk.document_id,
                        "chunk_index": chunk.chunk_index,
                        "page_no": chunk.page_no,
                        "heading_path": " > ".join(chunk.heading_path) if chunk.heading_path else "",
                        "image_refs": "|".join(chunk.image_refs) if chunk.image_refs else "",
                        "table_refs": "|".join(chunk.table_refs) if chunk.table_refs else "",
                        "image_urls": "|".join(img_url_map.get(iid, "") for iid in chunk.image_refs) if chunk.image_refs else "",
                        "has_table": chunk.has_table,
                        "has_code": chunk.has_code,
                    })

                await anyio.to_thread.run_sync(
                    vector_store.add_documents,
                    ids,
                    embeddings,
                    texts,
                    metadatas
                )

                # ── Step 6: Knowledge Graph Ingest (per file) ────────────────────────
                if settings.NEXUSRAG_ENABLE_KG:
                    try:
                        # Fetch workspace settings for KG customization
                        kb_result = await db.execute(select(KnowledgeBase).where(KnowledgeBase.id == workspace_id))
                        kb = kb_result.scalar_one_or_none()
                        
                        kg_service = KnowledgeGraphService(
                            workspace_id=workspace_id,
                            kg_entity_types=kb.kg_entity_types if kb else None,
                            kg_language=kb.kg_language if kb else None
                        )
                        full_text = "\n\n".join(texts)
                        await kg_service.ingest(full_text)
                    except Exception as e:
                        logger.error(f"[INGEST] KG ingest failed for {file_name}: {e}")

                # Mark doc as INDEXED (Only after EVERYTHING including KG is done)
                elapsed_ms = int((time.time() - start_time) * 1000)
                stmt = (
                    update(Document)
                    .where(Document.id == doc_id)
                    .values(
                        status=DocumentStatus.INDEXED, 
                        chunk_count=len(deduped_chunks),
                        processing_time_ms=elapsed_ms
                    )
                )
                await db.execute(stmt)
                await db.commit()
                logger.info(f"[INGEST] File {file_name} fully indexed in {elapsed_ms}ms")

        logger.info(f"[INGEST] Full pipeline done for workspace {workspace_id}")

        logger.info(f"[INGEST] Full pipeline done for workspace {workspace_id}")

    except Exception as e:
        logger.error(f"[INGEST] Fatal error in background task: {e}", exc_info=True)
        # Ensure any documents that were part of this batch don't stay in 'processing' forever
        try:
            async with AsyncSessionLocal() as db:
                stmt = (
                    update(Document)
                    .where(Document.id.in_(document_ids))
                    .where(Document.status.in_([DocumentStatus.PROCESSING, DocumentStatus.PARSING, DocumentStatus.INDEXING]))
                    .values(status=DocumentStatus.FAILED, error_message=f"Fatal pipeline error: {str(e)}")
                )
                await db.execute(stmt)
                await db.commit()
        except Exception as db_err:
            logger.error(f"[INGEST] Cleanup failed during fatal error: {db_err}")


@router.post("/")
async def upload_files(
    workspace_id: str = Form(...),
    files: List[UploadFile] = File(...),
):
    """
    **Upload files** và lưu trạng thái PENDING. Không xử lý ngay.
    """
    if not files:
        raise HTTPException(status_code=400, detail="No files provided")

    source_dir = os.path.join(DATA_BASE_PATH, str(workspace_id))
    os.makedirs(source_dir, exist_ok=True)
    
    file_names = []

    async with AsyncSessionLocal() as db:
        # Ensure Workspace exists
        kb_result = await db.execute(select(KnowledgeBase).where(KnowledgeBase.id == workspace_id))
        kb = kb_result.scalar_one_or_none()
        if not kb:
            kb = KnowledgeBase(id=workspace_id, name=f"Knowledge Base {workspace_id}")
            db.add(kb)
            await db.commit()

        for file in files:
            safe_filename = os.path.basename(file.filename or "upload")
            temp_path = os.path.join(source_dir, safe_filename)
            
            with open(temp_path, "wb") as f:
                while chunk := await file.read(1024 * 1024):  # 1MB chunks
                    f.write(chunk)
            
            file_size = os.path.getsize(temp_path)

            # Check if document already exists
            doc_result = await db.execute(
                select(Document)
                .where(Document.workspace_id == workspace_id)
                .where(Document.original_filename == safe_filename)
            )
            existing_doc = doc_result.scalar_one_or_none()

            if existing_doc:
                existing_doc.status = DocumentStatus.PENDING
                existing_doc.file_size = file_size
            else:
                doc = Document(
                    workspace_id=workspace_id,
                    filename=safe_filename,
                    original_filename=safe_filename,
                    file_type=os.path.splitext(safe_filename)[1].lower(),
                    file_size=file_size,
                    status=DocumentStatus.PENDING,
                    parser_version="docling"
                )
                db.add(doc)
            
            file_names.append(safe_filename)
            await db.commit()

        logger.info(f"[UPLOAD] Saved {len(files)} files for workspace {workspace_id} with status PENDING: {file_names}")

        return JSONResponse(content={
            "status": "success",
            "message": "Files received and saved. Awaiting processing.",
            "workspace_id": workspace_id,
            "files": file_names,
            "file_count": len(file_names),
        })

@router.post("/process")
async def process_files(
    background_tasks: BackgroundTasks,
    workspace_id: str,
    filenames: Optional[str] = None,
    force: bool = False
):
    """
    Kích hoạt pipeline xử lý cho các file đang ở trạng thái PENDING.
    """
    async with AsyncSessionLocal() as db:
        status_filter = [DocumentStatus.PENDING, DocumentStatus.FAILED]
        if force:
            status_filter.append(DocumentStatus.INDEXED)

        query = select(Document).where(
            Document.workspace_id == workspace_id
        ).where(
            Document.status.in_(status_filter)
        )
        if filenames:
            file_list = [f.strip() for f in filenames.split(",")]
            query = query.where(Document.original_filename.in_(file_list))
            
        doc_result = await db.execute(query)
        docs_to_process = doc_result.scalars().all()
        
        if not docs_to_process:
             return JSONResponse(content={
                "status": "success",
                "message": "No pending files found to process."
            })
             
        file_paths = []
        file_names = []
        document_ids = []
        
        source_dir = os.path.join(DATA_BASE_PATH, str(workspace_id))
        
        for doc in docs_to_process:
            path = os.path.join(source_dir, doc.original_filename)
            if os.path.exists(path):
                file_paths.append(path)
                file_names.append(doc.original_filename)
                document_ids.append(doc.id)
                # Cập nhật trạng thái ngay lập tức
                doc.status = DocumentStatus.PROCESSING
            else:
                doc.status = DocumentStatus.FAILED
                doc.error_message = "File physically missing"
        
        await db.commit()

        if file_paths:
            # Chạy pipeline trong threadpool để tránh chặn Event Loop chính
            background_tasks.add_task(
                process_file_background,
                workspace_id=workspace_id,
                file_paths=file_paths,
                file_names=file_names,
                document_ids=document_ids
            )
            
        return JSONResponse(content={
            "status": "success",
            "message": f"Processing started for {len(file_paths)} files.",
            "processing": file_names
        })
