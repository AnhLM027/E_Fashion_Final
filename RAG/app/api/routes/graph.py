from fastapi import APIRouter, HTTPException, Query
from typing import List, Optional
from app.services.knowledge_graph_service import KnowledgeGraphService
import logging

router = APIRouter()
logger = logging.getLogger(__name__)

@router.get("/entities/{workspace_id}")
async def get_entities(
    workspace_id: str,
    search: Optional[str] = None,
    entity_type: Optional[str] = None,
    limit: int = 50,
    offset: int = 0
):
    """Lấy danh sách các thực thể (entities) được trích xuất từ đồ thị tri thức."""
    try:
        kg = KnowledgeGraphService(workspace_id=workspace_id)
        return await kg.get_entities(search=search, entity_type=entity_type, limit=limit, offset=offset)
    except Exception as e:
        logger.error(f"get_entities failed for workspace {workspace_id}: {e}")
        return []

@router.get("/relationships/{workspace_id}")
async def get_relationships(
    workspace_id: str,
    entity_name: Optional[str] = None,
    limit: int = 50
):
    """Lấy danh sách các mối quan hệ (relationships) giữa các thực thể."""
    try:
        kg = KnowledgeGraphService(workspace_id=workspace_id)
        return await kg.get_relationships(entity_name=entity_name, limit=limit)
    except Exception as e:
        logger.error(f"get_relationships failed for workspace {workspace_id}: {e}")
        return []

@router.get("/visualization/{workspace_id}")
async def get_graph_data(
    workspace_id: str,
    center_entity: Optional[str] = None,
    depth: int = 3,
    max_nodes: int = 150
):
    """Lấy dữ liệu đồ thị dạng JSON (nodes/edges) để hiển thị trên Frontend."""
    try:
        kg = KnowledgeGraphService(workspace_id=workspace_id)
        return await kg.get_graph_data(center_entity=center_entity, max_depth=depth, max_nodes=max_nodes)
    except Exception as e:
        logger.error(f"get_graph_data failed for workspace {workspace_id}: {e}")
        return {"nodes": [], "edges": [], "is_truncated": False}

@router.get("/analytics/{workspace_id}")
async def get_analytics(workspace_id: str):
    """Thống kê chi tiết về độ phủ và chất lượng của đồ thị tri thức."""
    try:
        kg = KnowledgeGraphService(workspace_id=workspace_id)
        return await kg.get_analytics()
    except Exception as e:
        logger.error(f"get_analytics failed for workspace {workspace_id}: {e}")
        return {"entity_count": 0, "relationship_count": 0}
