import logging
import uuid
import hashlib
from typing import Sequence, Optional, List
from qdrant_client.models import Distance, VectorParams, PointStruct, Filter, FieldCondition, MatchValue

from app.db.qdrant import get_qdrant_client
from app.core.config import settings
from app.services.embedder import get_embedding_service

logger = logging.getLogger(__name__)

def _generate_uuid(id_str: str) -> str:
    """Qdrant requires valid UUIDs. If source ID isn't a valid UUID, convert it deterministically."""
    try:
        uuid.UUID(id_str)
        return id_str
    except ValueError:
        return str(uuid.uuid5(uuid.NAMESPACE_OID, id_str))

class VectorStore:
    """
    Vector store service for managing document embeddings in Qdrant.
    Each workspace has its own collection for namespace isolation.
    """
    COLLECTION_PREFIX = "kb_"

    def __init__(self, workspace_id: int):
        self.workspace_id = str(workspace_id)
        self.collection_name = f"{self.COLLECTION_PREFIX}{workspace_id}"
        self._ensure_collection()

    def _ensure_collection(self) -> None:
        client = get_qdrant_client()
        if not client.collection_exists(self.collection_name):
            client.create_collection(
                collection_name=self.collection_name,
                vectors_config=VectorParams(
                    size=get_embedding_service().dimension, # Match with actual embedding model
                    distance=Distance.COSINE
                )
            )
            logger.info(f"Created Qdrant collection {self.collection_name}")

    def add_documents(
        self,
        ids: Sequence[str],
        embeddings: Sequence[list[float]],
        documents: Sequence[str],
        metadatas: Sequence[dict] | None = None
    ) -> None:
        if not ids:
            return

        client = get_qdrant_client()
        points = []
        for i in range(len(ids)):
            q_id = _generate_uuid(ids[i])
            doc = documents[i]
            meta = metadatas[i] if metadatas else {}
            
            # Pack document content into payload alongside metadata
            payload = {"document": doc, "original_id": ids[i]}
            payload.update(meta)

            points.append(
                PointStruct(
                    id=q_id,
                    vector=embeddings[i],
                    payload=payload
                )
            )

        client.upsert(
            collection_name=self.collection_name,
            points=points
        )
        logger.info(f"Added {len(ids)} documents into Qdrant collection {self.collection_name}")

    def query(
        self,
        query_embedding: list[float],
        n_results: int = 5,
        where: dict | None = None,
        include: list[str] | None = None
    ) -> dict:
        client = get_qdrant_client()

        query_filter = None
        if where:
            conditions = []
            for k, v in where.items():
                conditions.append(FieldCondition(key=k, match=MatchValue(value=v)))
            query_filter = Filter(must=conditions)

        search_result = client.query_points(
            collection_name=self.collection_name,
            query=query_embedding,
            query_filter=query_filter,
            limit=n_results,
            with_payload=True
        ).points

        out_ids = []
        out_documents = []
        out_metadatas = []
        out_distances = []

        for hit in search_result:
            payload = hit.payload or {}
            out_ids.append(payload.get("original_id", str(hit.id)))
            out_documents.append(payload.pop("document", ""))
            
            # The remaining payload is the metadata
            payload.pop("original_id", None)
            out_metadatas.append(payload)
            out_distances.append(hit.score)

        return {
            "ids": out_ids,
            "documents": out_documents,
            "metadatas": out_metadatas,
            "distances": out_distances
        }

    def delete_by_document_id(self, document_id: int) -> None:
        client = get_qdrant_client()
        client.delete(
            collection_name=self.collection_name,
            points_selector=Filter(
                must=[FieldCondition(key="document_id", match=MatchValue(value=document_id))]
            )
        )
        logger.info(f"Deleted chunks for document {document_id} from {self.collection_name}")

    def delete_collection(self) -> None:
        client = get_qdrant_client()
        client.delete_collection(collection_name=self.collection_name)
        logger.info(f"Deleted Qdrant collection {self.collection_name}")

    def count(self) -> int:
        client = get_qdrant_client()
        info = client.get_collection(collection_name=self.collection_name)
        return info.points_count

    def get_by_ids(self, ids: Sequence[str]) -> dict:
        client = get_qdrant_client()
        q_ids = [_generate_uuid(id) for id in ids]
        
        points, _ = client.retrieve(
            collection_name=self.collection_name,
            ids=q_ids,
            with_payload=True
        )
        
        out_documents = []
        out_metadatas = []
        for p in points:
            payload = p.payload or {}
            out_documents.append(payload.pop("document", ""))
            payload.pop("original_id", None)
            out_metadatas.append(payload)
            
        return {
            "documents": [out_documents],
            "metadatas": [out_metadatas]
        }

def get_vector_store(workspace_id: int) -> VectorStore:
    return VectorStore(workspace_id)
