"""
MBA_RAG Orchestrator - NexusRAG Pipeline
==========================================
Module: app/services/rag_orchestrator.py

Mô tả:
    Orchestrator này chuyển luồng RAG hiện tại sang NexusRAG-style retrieval
    trong khi vẫn giữ Qdrant làm vector store.
"""

import logging
from typing import Optional

from app.core.config import settings
from app.services.nexus_rag_service import NexusRAGService
from app.services.models.parsed_document import DeepRetrievalResult

logger = logging.getLogger(__name__)


class RAGOrchestrator:
    """
    RAG Orchestrator: Delegates to NexusRAGService for hybrid retrieval.

    Pipeline:
        KG query + Vector over-fetch -> Cross-encoder rerank -> Structured context
    """

    def __init__(self, workspace_id: int):
        self.workspace_id = workspace_id
        self.rag_service = NexusRAGService(workspace_id=workspace_id)

    async def retrieve_context(
        self,
        query: str,
        top_k: Optional[int] = None,
        prefetch_k: Optional[int] = None,
        min_score: Optional[float] = None,
    ) -> DeepRetrievalResult:
        if top_k is None:
            top_k = settings.NEXUSRAG_RERANKER_TOP_K
        if prefetch_k is None:
            prefetch_k = settings.NEXUSRAG_VECTOR_PREFETCH

        logger.info(f"[RAG] Query: '{query[:60]}...', top_k={top_k}, prefetch={prefetch_k}")

        return await self.rag_service.query_deep(
            question=query,
            top_k=top_k,
            prefetch_k=prefetch_k,
            mode="hybrid",
            min_score=min_score,
        )
