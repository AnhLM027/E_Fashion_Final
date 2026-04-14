import logging
from typing import List, Optional
from fastapi import APIRouter, HTTPException, Query
from app.db.mongodb import (
    get_chat_history_by_session, 
    delete_chat_session, 
    get_ami_debate_leaderboard,
    get_user_chat_sessions
)

logger = logging.getLogger(__name__)

router = APIRouter()

@router.get("/user/{user_id}/sessions/grouped")
async def get_user_sessions_grouped_api(
    user_id: str, 
    limit: int = 30, 
    skip: int = Query(0, ge=0),
    source: Optional[str] = Query(None)
):
    """Lấy danh sách các session chat gộp nhóm của một user (Dùng cho Ami)."""
    try:
        from app.db.mongodb import get_user_sessions_grouped
        sessions, total = get_user_sessions_grouped(user_id, limit, skip, source)
        return {
            "status": "ok", 
            "sessions": sessions, 
            "chat_history": sessions,
            "total_returned": total
        }
    except Exception as e:
        logger.error(f"Error fetching grouped user sessions: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/user/{user_id}/sessions")
async def get_user_sessions(
    user_id: str, 
    limit: int = 30, 
    skip: int = Query(0, ge=0),
    source: Optional[str] = Query(None)
):
    """Lấy danh sách các session chat của một user."""
    try:
        from app.db.mongodb import get_user_sessions
        sessions, total = get_user_sessions(user_id, limit, skip, source)
        return {
            "status": "ok", 
            "sessions": sessions, 
            "chat_history": sessions,
            "total_returned": total
        }
    except Exception as e:
        logger.error(f"Error fetching user sessions: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/session/{session_id}")
async def get_session_history(session_id: str):
    """Lấy toàn bộ tin nhắn trong một session."""
    try:
        history = get_chat_history_by_session(session_id)
        if not history:
            return {"status": "success", "history": [], "message": "Session empty or not found"}
        
        # Mongodb objects are not serializable by default
        serializable_history = []
        for msg in history:
            msg["_id"] = str(msg["_id"])
            serializable_history.append(msg)
            
        return {
            "status": "success", 
            "history": serializable_history,
            "chat_history": serializable_history
        }
    except Exception as e:
        logger.error(f"Error fetching history: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.delete("/session/{session_id}")
async def remove_session(session_id: str):
    """Xóa một session chat cụ thể."""
    try:
        deleted_count = delete_chat_session(session_id)
        return {"status": "success", "deleted_count": deleted_count}
    except Exception as e:
        logger.error(f"Error deleting session: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/leaderboard/{source}")
async def get_leaderboard(source: str, limit: int = Query(10, ge=1, le=50)):
    """Lấy bảng xếp hạng tinh túy nhất của môn học."""
    try:
        leaderboard = get_ami_debate_leaderboard(source, limit)
        for entry in leaderboard:
            entry["_id"] = str(entry["_id"])
        return {"status": "success", "leaderboard": leaderboard}
    except Exception as e:
        logger.error(f"Error fetching leaderboard: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/source/{source}/users")
async def get_users_by_source(
    source: str, 
    limit: int = Query(20, ge=1, le=100), 
    skip: int = Query(0, ge=0)
):
    """Lấy danh sách người dùng đã từng nhắn tin trong một source/workspace."""
    try:
        from app.db.mongodb import get_source_users
        users, total = get_source_users(source, limit, skip)
        return {
            "status": "ok", 
            "source": source,
            "total_users": total,
            "users": users
        }
    except Exception as e:
        logger.error(f"Error fetching source users: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@router.delete("/user/{user_id}")
async def remove_user_history(user_id: str, source: Optional[str] = Query(None)):
    """Xóa toàn bộ lịch sử chat của một user (theo source hoặc tất cả)."""
    try:
        from app.db.mongodb import delete_user_chat_history
        deleted_count = delete_user_chat_history(user_id, source)
        return {"status": "success", "deleted_count": deleted_count}
    except Exception as e:
        logger.error(f"Error deleting user history: {e}")
        raise HTTPException(status_code=500, detail=str(e))
