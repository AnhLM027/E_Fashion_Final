"""
Deep Retriever
===============

Hybrid retrieval combining Knowledge Graph (LightRAG) + Vector Search (Qdrant)
+ Cross-encoder Reranking (BGE reranker).
"""
from __future__ import annotations

import asyncio
import logging
from typing import Optional
from sqlalchemy import select

from app.core.config import settings
from app.core.database import AsyncSessionLocal
from app.models.document import DocumentImage, DocumentTable
from app.services.embedder import get_embedding_service
from app.services.knowledge_graph_service import KnowledgeGraphService
from app.services.models.parsed_document import (
    Citation,
    DeepRetrievalResult,
    EnrichedChunk,
    ExtractedImage,
    ExtractedTable,
)
from app.services.reranker import get_reranker_service
from app.services.vector_store import VectorStore

logger = logging.getLogger(__name__)


class DeepRetriever:
    """Hybrid retriever: KG traversal + vector similarity + cross-encoder reranking."""

    def __init__(
        self,
        workspace_id: int,
        kg_service: Optional[KnowledgeGraphService],
        vector_store: VectorStore,
        embedder=None,
        db=None,
        reranker=None,
    ):
        self.workspace_id = workspace_id
        self.kg_service = kg_service
        self.vector_store = vector_store
        self.embedder = embedder or get_embedding_service()
        self.db = db
        self.reranker = reranker or get_reranker_service()

    async def query(
        self,
        question: str,
        mode: str = "hybrid",
        top_k: int = 5,
        prefetch_k: Optional[int] = None,
        document_ids: Optional[list[int]] = None,
        metadata_filter: Optional[dict] = None,
        include_images: bool = True,
        min_score: Optional[float] = None,
    ) -> DeepRetrievalResult:
        """Execute hybrid retrieval with reranking and optional KG enrichment."""
        if prefetch_k is None:
            prefetch_k = max(settings.NEXUSRAG_VECTOR_PREFETCH, top_k * 3)

        kg_task = None
        if self.kg_service and mode != "vector_only":
            kg_task = asyncio.create_task(self._kg_query(question, mode))

        vector_task = asyncio.create_task(
            asyncio.to_thread(
                self._vector_query,
                question,
                prefetch_k,
                document_ids,
                metadata_filter,
            )
        )

        kg_summary = ""
        if kg_task:
            try:
                kg_summary = await kg_task
            except Exception as exc:
                logger.warning(f"KG query failed, continuing with vector only: {exc}")

        raw_chunks, raw_citations = await vector_task
        chunks, citations = await asyncio.to_thread(
            self._rerank_chunks,
            question,
            raw_chunks,
            raw_citations,
            top_k,
            min_score or settings.NEXUSRAG_MIN_RELEVANCE_SCORE,
        )

        image_refs: list[ExtractedImage] = []
        table_refs: list[ExtractedTable] = []
        if include_images and chunks:
            page_refs = {(c.document_id, c.page_no) for c in chunks if c.document_id and c.page_no}
            if page_refs:
                if self.db is not None:
                    image_refs = await self._find_related_images(page_refs, self.db)
                    table_refs = await self._find_related_tables(page_refs, self.db)
                else:
                    async with AsyncSessionLocal() as db:
                        image_refs = await self._find_related_images(page_refs, db)
                        table_refs = await self._find_related_tables(page_refs, db)

        context = self._assemble_context(chunks, citations, kg_summary, image_refs, table_refs)

        return DeepRetrievalResult(
            chunks=chunks,
            citations=citations,
            context=context,
            query=question,
            mode=mode,
            knowledge_graph_summary=kg_summary,
            image_refs=image_refs,
            table_refs=table_refs,
        )

    async def _kg_query(self, question: str, mode: str) -> str:
        """Get raw KG context relevant to the question."""
        if not self.kg_service:
            return ""

        try:
            return await asyncio.wait_for(
                self.kg_service.get_relevant_context(question),
                timeout=settings.NEXUSRAG_KG_QUERY_TIMEOUT,
            )
        except asyncio.TimeoutError:
            logger.warning("KG raw context retrieval timed out")
            return ""
        except Exception as exc:
            logger.warning(f"KG raw context failed: {exc}")
            return ""

    def _vector_query(
        self,
        question: str,
        top_k: int,
        document_ids: Optional[list[int]],
        metadata_filter: Optional[dict] = None,
    ) -> tuple[list[EnrichedChunk], list[Citation]]:
        """Synchronous vector search via Qdrant (over-fetch stage)."""
        query_embedding = self.embedder.embed_query(question)

        where = metadata_filter.copy() if metadata_filter else {}
        if document_ids:
            where["document_id"] = document_ids
        if not where:
            where = None

        results = self.vector_store.query(
            query_embedding=query_embedding,
            n_results=top_k,
            where=where,
        )

        chunks: list[EnrichedChunk] = []
        citations: list[Citation] = []

        for i, doc_text in enumerate(results.get("documents", [])):
            meta = results["metadatas"][i] if results.get("metadatas") else {}
            heading_path = []
            heading_str = meta.get("heading_path", "")
            if isinstance(heading_str, str) and heading_str:
                heading_path = heading_str.split(" > ")

            image_refs = []
            image_data = meta.get("image_refs") or meta.get("image_ids") or ""
            if isinstance(image_data, str):
                image_refs = [item for item in image_data.split("|") if item]
            elif isinstance(image_data, list):
                image_refs = [str(item) for item in image_data if item]

            table_refs = []
            table_data = meta.get("table_refs") or meta.get("table_ids") or ""
            if isinstance(table_data, str):
                table_refs = [item for item in table_data.split("|") if item]
            elif isinstance(table_data, list):
                table_refs = [str(item) for item in table_data if item]

            chunk = EnrichedChunk(
                content=doc_text,
                chunk_index=meta.get("chunk_index", i),
                source_file=meta.get("file_name") or meta.get("source") or "Tài liệu",
                document_id=int(meta.get("document_id", 0)) if meta.get("document_id") else 0,
                page_no=int(meta.get("page_no", 0)) if meta.get("page_no") else 0,
                heading_path=heading_path,
                image_refs=image_refs,
                table_refs=table_refs,
                has_table=meta.get("has_table", False),
                has_code=meta.get("has_code", False),
            )
            chunks.append(chunk)
            citations.append(Citation(
                source_file=chunk.source_file,
                document_id=chunk.document_id,
                page_no=chunk.page_no,
                heading_path=heading_path,
            ))

        return chunks, citations

    def _rerank_chunks(
        self,
        question: str,
        chunks: list[EnrichedChunk],
        citations: list[Citation],
        top_k: int,
        min_score: float,
    ) -> tuple[list[EnrichedChunk], list[Citation]]:
        """Cross-encoder reranking and relevance filtering."""
        if not chunks:
            return [], []

        doc_texts = [c.content for c in chunks]
        reranked = self.reranker.rerank(
            query=question,
            documents=doc_texts,
            top_k=top_k,
            min_score=min_score,
        )

        if not reranked:
            logger.warning(
                f"Reranker filtered all {len(chunks)} chunks below threshold {min_score}, falling back to top 3"
            )
            return chunks[: min(3, len(chunks))], citations[: min(3, len(citations))]

        reranked_chunks = [chunks[item.index] for item in reranked]
        reranked_citations = [citations[item.index] for item in reranked]

        logger.info(
            f"Reranked {len(chunks)} → {len(reranked_chunks)} chunks "
            f"(scores: {reranked[0].score:.3f} → {reranked[-1].score:.3f})"
        )
        return reranked_chunks, reranked_citations

    async def _find_related_images(
        self,
        page_refs: set[tuple[int, int]],
        db,
    ) -> list[ExtractedImage]:
        images: list[ExtractedImage] = []
        for doc_id, page_no in page_refs:
            image_res = await db.execute(
                select(DocumentImage).where(
                    DocumentImage.document_id == doc_id,
                    DocumentImage.page_no == page_no,
                )
            )
            for img in image_res.scalars().all():
                images.append(ExtractedImage(
                    image_id=img.image_id,
                    document_id=img.document_id,
                    page_no=img.page_no,
                    file_path=img.file_path,
                    caption=img.caption,
                    width=img.width,
                    height=img.height,
                ))

        seen = set()
        unique = []
        for img in images:
            if img.image_id not in seen:
                seen.add(img.image_id)
                unique.append(img)
        return unique

    async def _find_related_tables(
        self,
        page_refs: set[tuple[int, int]],
        db,
    ) -> list[ExtractedTable]:
        tables: list[ExtractedTable] = []
        for doc_id, page_no in page_refs:
            table_res = await db.execute(
                select(DocumentTable).where(
                    DocumentTable.document_id == doc_id,
                    DocumentTable.page_no == page_no,
                )
            )
            for tbl in table_res.scalars().all():
                tables.append(ExtractedTable(
                    table_id=tbl.table_id,
                    document_id=tbl.document_id,
                    page_no=tbl.page_no,
                    content_markdown=tbl.content_markdown,
                    caption=tbl.caption,
                    num_rows=tbl.num_rows,
                    num_cols=tbl.num_cols,
                ))

        seen = set()
        unique = []
        for tbl in tables:
            if tbl.table_id not in seen:
                seen.add(tbl.table_id)
                unique.append(tbl)
        return unique

    @staticmethod
    def _assemble_context(
        chunks: list[EnrichedChunk],
        citations: list[Citation],
        kg_summary: str,
        image_refs: list[ExtractedImage],
        table_refs: list[ExtractedTable],
    ) -> str:
        parts: list[str] = []

        if kg_summary:
            parts.append("## Knowledge Graph Insights")
            parts.append(kg_summary)
            parts.append("")

        if chunks:
            parts.append("## Retrieved Document Sections")
            for i, (chunk, citation) in enumerate(zip(chunks, citations)):
                parts.append(f"### [{i + 1}] {citation.format()}")
                parts.append(chunk.content)
                parts.append("")

        if image_refs:
            parts.append("## Available Document Images")
            for img in image_refs:
                caption_str = f': "{img.caption}"' if img.caption else ""
                parts.append(f"- Image p.{img.page_no}{caption_str} (id: {img.image_id})")
            parts.append("")

        if table_refs:
            parts.append("## Available Document Tables")
            for tbl in table_refs:
                caption_str = f': "{tbl.caption}"' if tbl.caption else ""
                parts.append(
                    f"- Table p.{tbl.page_no} ({tbl.num_rows}x{tbl.num_cols}){caption_str}"
                )
            parts.append("")

        if not parts:
            return "No relevant documents found."

        return "\n".join(parts)
