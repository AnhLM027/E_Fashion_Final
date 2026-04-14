"""
NexusRAG Service Adapter
=========================

High-level wrapper exposing the NexusRAG-style deep retrieval API while
keeping Qdrant as the vector store.
"""
from __future__ import annotations

from typing import Optional

from app.core.config import settings
from app.services.deep_retriever import DeepRetriever
from app.services.embedder import get_embedding_service
from app.services.knowledge_graph_service import KnowledgeGraphService
from app.services.reranker import get_reranker_service
from app.services.vector_store import get_vector_store
from app.services.models.parsed_document import DeepRetrievalResult


class NexusRAGService:
    """Wrapper around DeepRetriever with NexusRAG-compatible query APIs."""

    def __init__(
        self,
        workspace_id: int,
        kg_language: Optional[str] = None,
        kg_entity_types: Optional[list[str]] = None,
    ):
        self.workspace_id = workspace_id
        self.kg_service: Optional[KnowledgeGraphService] = None
        if settings.NEXUSRAG_ENABLE_KG:
            try:
                self.kg_service = KnowledgeGraphService(
                    workspace_id=workspace_id,
                    kg_language=kg_language,
                    kg_entity_types=kg_entity_types,
                )
            except Exception:
                self.kg_service = None

        self.embedder = get_embedding_service()
        self.vector_store = get_vector_store(workspace_id)
        self.reranker = get_reranker_service()
        self.retriever = DeepRetriever(
            workspace_id=workspace_id,
            kg_service=self.kg_service,
            vector_store=self.vector_store,
            embedder=self.embedder,
            reranker=self.reranker,
        )

    async def query_deep(
        self,
        question: str,
        top_k: int = 5,
        prefetch_k: Optional[int] = None,
        mode: str = "hybrid",
        document_ids: Optional[list[int]] = None,
        metadata_filter: Optional[dict] = None,
        include_images: bool = True,
        min_score: Optional[float] = None,
    ) -> DeepRetrievalResult:
        """Execute a full hybrid retrieval query."""
        return await self.retriever.query(
            question=question,
            mode=mode,
            top_k=top_k,
            prefetch_k=prefetch_k,
            document_ids=document_ids,
            metadata_filter=metadata_filter,
            include_images=include_images,
            min_score=min_score,
        )

    def query(
        self,
        question: str,
        top_k: int = 5,
        document_ids: Optional[list[int]] = None,
        metadata_filter: Optional[dict] = None,
    ):
        """Backward-compatible vector-only query."""
        query_embedding = self.embedder.embed_query(question)
        where = metadata_filter.copy() if metadata_filter else {}
        if document_ids:
            where["document_id"] = document_ids
        if not where:
            where = None

        return self.vector_store.query(
            query_embedding=query_embedding,
            n_results=top_k,
            where=where,
        )
