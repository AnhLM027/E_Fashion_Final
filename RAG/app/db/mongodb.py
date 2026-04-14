import logging
from datetime import datetime
from pymongo import MongoClient
from app.core.config import settings

logger = logging.getLogger(__name__)

_mongo_client = None

def get_mongo_client():
    global _mongo_client
    if _mongo_client is None:
        try:
            _mongo_client = MongoClient(settings.MONGO_URI)
            logger.info("Connected to MongoDB successfully.")
        except Exception as e:
            logger.error(f"Failed to connect to MongoDB: {e}")
    return _mongo_client

def get_chat_collection():
    client = get_mongo_client()
    if client:
        db = client[settings.MONGO_DB_NAME]
        return db["chat_history"]
    return None

def get_debate_collection():
    client = get_mongo_client()
    if client:
        db = client[settings.MONGO_DB_NAME]
        return db["debate_results"]
    return None

def save_chat_message(
    user_id: str,
    session_id: str,
    message: str,
    response: str,
    source: str,
    thinking: str = "",
    sources: list[dict] | None = None,
    search_query: str = ""
):
    col = get_chat_collection()
    if col is not None:
        doc = {
            "user_id": user_id,
            "session_id": session_id,
            "message": message,
            "response": response,
            "thinking": thinking,
            "source": source,
            "search_query": search_query,
            "sources": sources or [],
            "timestamp": datetime.utcnow()
        }
        col.insert_one(doc)

def get_chat_history_by_session(session_id: str):
    col = get_chat_collection()
    if col is not None:
        return list(col.find({"session_id": session_id}).sort("timestamp", 1))
    return []

def delete_chat_session(session_id: str):
    col = get_chat_collection()
    if col is not None:
        result = col.delete_many({"session_id": session_id})
        return result.deleted_count
    return 0

def delete_chat_history_by_source(source: str):
    """Xóa toàn bộ lịch sử chat của một workspace/source."""
    col = get_chat_collection()
    if col is not None:
        result = col.delete_many({"source": source})
        logger.info(f"Deleted {result.deleted_count} messages from MongoDB for source {source}")
        return result.deleted_count
    return 0

def save_ami_debate_result(user_id: str, user_name: str, source: str, score: int, **kwargs):
    col = get_debate_collection()
    if col is not None:
        doc = {
            "user_id": user_id,
            "user_name": user_name,
            "source": source,
            "score": score,
            "timestamp": datetime.utcnow(),
            **kwargs
        }
        res = col.insert_one(doc)
        return str(res.inserted_id)
    return None

def delete_debate_results_by_source(source: str):
    """Xóa kết quả thi đấu/debate của một workspace/source."""
    col = get_debate_collection()
    if col is not None:
        result = col.delete_many({"source": source})
        logger.info(f"Deleted {result.deleted_count} debate results from MongoDB for source {source}")
        return result.deleted_count
    return 0

def get_ami_debate_leaderboard(source: str, limit: int = 10):
    col = get_debate_collection()
    if col is not None:
        # Nhóm theo user và lấy điểm cao nhất
        pipeline = [
            {"$match": {"source": source}},
            {"$sort": {"score": -1}},
            {"$group": {
                "_id": "$user_id",
                "user_name": {"$first": "$user_name"},
                "score": {"$first": "$score"},
                "timestamp": {"$first": "$timestamp"}
            }},
            {"$sort": {"score": -1}},
            {"$limit": limit}
        ]
        return list(col.aggregate(pipeline))
    return []
    
def get_user_chat_sessions(user_id: str, limit: int = 30, source: str = None):
    col = get_chat_collection()
    if col is not None:
        match_query = {"user_id": user_id}
        if source:
            match_query["source"] = source
            
        pipeline = [
            {"$match": match_query},
            {"$sort": {"timestamp": -1}},
            {"$group": {
                "_id": "$session_id",
                "last_message": {"$first": "$message"},
                "last_timestamp": {"$first": "$timestamp"},
                "message_count": {"$sum": 1},
                "source": {"$first": "$source"}
            }},
            {"$sort": {"last_timestamp": -1}},
            {"$limit": limit}
        ]
        results = list(col.aggregate(pipeline))
        for res in results:
            res["session_id"] = res["_id"]
            del res["_id"]
            # Convert datetime to string
            if isinstance(res["last_timestamp"], datetime):
                res["last_timestamp"] = res["last_timestamp"].isoformat()
        return results
    return []

def get_source_users(source: str, limit: int = 50, skip: int = 0):
    col = get_chat_collection()
    if col is not None:
        pipeline = [
            {"$match": {"source": source}},
            {"$sort": {"timestamp": -1}}, # Sắp xếp mới nhất lên đầu
            {
                "$group": {
                    "_id": "$user_id",
                    "last_active": {"$first": "$timestamp"},
                    "last_message": {"$first": "$message"},
                    "first_active": {"$last": "$timestamp"},
                    "first_message": {"$last": "$message"},
                    "message_count": {"$sum": 1},
                    "sessions": {"$addToSet": "$session_id"},
                    "source": {"$first": "$source"}
                }
            },
            {"$project": {
                "user_id": "$_id",
                "last_active": 1,
                "last_message": 1,
                "first_active": 1,
                "first_message": 1,
                "message_count": 1,
                "session_count": {"$size": "$sessions"},
                "source": 1
            }},
            {"$sort": {"last_active": -1}},
            {"$skip": skip},
            {"$limit": limit}
        ]
        
        results = list(col.aggregate(pipeline))
        total = len(list(col.distinct("user_id", {"source": source})))
        
        formatted_users = []
        for res in results:
            last_ts = res["last_active"].isoformat() if isinstance(res["last_active"], datetime) else None
            first_ts = res["first_active"].isoformat() if isinstance(res["first_active"], datetime) else None
            
            formatted_users.append({
                "user_id": str(res["user_id"]),
                "last_active": last_ts,
                "last_timestamp": last_ts,
                "last_message": str(res.get("last_message") or ""),
                "first_active": first_ts,
                "first_timestamp": first_ts,
                "first_message": str(res.get("first_message") or ""),
                "message_count": int(res.get("message_count", 0)),
                "session_count": int(res.get("session_count", 0)),
                "source": res.get("source") or source
            })
            
        return formatted_users, total
    return [], 0

def delete_user_chat_history(user_id: str, source: str = None):
    """Xóa lịch sử chat dựa trên user_id và tùy chọn source."""
    col = get_chat_collection()
    if col is not None:
        query = {"user_id": user_id}
        if source:
            query["source"] = source
        result = col.delete_many(query)
        logger.info(f"Deleted {result.deleted_count} messages from MongoDB for user {user_id} (source: {source})")
        return result.deleted_count
    return 0

def get_user_sessions_grouped(user_id: str, limit: int = 20, skip: int = 0, source: str = None):
    """Lấy danh sách các session chat của một user (đã gộp nhóm)."""
    col = get_chat_collection()
    if col is not None:
        effective_source = None if source == "undefined" else source
        
        match_query = {"user_id": user_id}
        if effective_source:
            match_query["source"] = effective_source
            
        pipeline = [
            {"$match": match_query},
            {"$sort": {"timestamp": -1}},
            {
                "$group": {
                    "_id": "$session_id",
                    "session_id": {"$first": "$session_id"},
                    "last_message": {"$first": "$message"},
                    "last_timestamp": {"$first": "$timestamp"},
                    "first_message": {"$last": "$message"},
                    "message_count": {"$sum": 1},
                    "source": {"$first": "$source"}
                }
            },
            {"$sort": {"last_timestamp": -1}},
            {"$skip": skip},
            {"$limit": limit}
        ]
        
        sessions = []
        results = list(col.aggregate(pipeline))
        for res in results:
            if isinstance(res.get("last_timestamp"), datetime):
                res["last_timestamp"] = res["last_timestamp"].isoformat()
            sessions.append(res)
            
        total = len(list(col.distinct("session_id", match_query)))
        return sessions, total
    return [], 0

def get_user_sessions(user_id: str, limit: int = 20, skip: int = 0, source: str = None):
    """Lấy danh sách PHẲNG toàn bộ tin nhắn của một user (cho trang Admin)."""
    col = get_chat_collection()
    if col is not None:
        effective_source = None if source == "undefined" else source
        query = {"user_id": user_id}
        if effective_source:
            query["source"] = effective_source
            
        total = col.count_documents(query)
        cursor = col.find(query).sort("timestamp", -1).skip(skip).limit(limit)
        
        messages = []
        for doc in cursor:
            doc["_id"] = str(doc["_id"])
            if "timestamp" in doc and isinstance(doc["timestamp"], datetime):
                doc["timestamp"] = doc["timestamp"].isoformat()
            messages.append(doc)
            
        return messages, total
    return [], 0

def get_chatbot_name(source: str) -> str | None:
    """Lấy tên Chatbot từ collection 'chatbots' dựa trên source id."""
    client = get_mongo_client()
    if client:
        db = client[settings.MONGO_DB_NAME]
        col = db["chatbots"]
        logger.info(f"MongoDB Query: source={source} in DB={settings.MONGO_DB_NAME}")
        bot = col.find_one({"source": source}, {"name": 1})
        if bot:
            logger.info(f"MongoDB Result: Found chatbot name '{bot.get('name')}'")
            return bot.get("name")
        else:
            logger.warning(f"MongoDB Result: No chatbot found for source='{source}'")
    return None
