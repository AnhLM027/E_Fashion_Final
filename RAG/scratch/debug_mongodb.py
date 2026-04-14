from pymongo import MongoClient
import os

try:
    client = MongoClient("mongodb://localhost:27018/")
    db = client["mba_mini"]
    col = db["chat_history"]
    
    source = "admin37989"
    total_count = col.count_documents({})
    print(f"Total documents in chat_history: {total_count}")
    
    distinct_users = col.distinct("user_id", {"source": source})
    print(f"Distinct users for source {source}: {len(distinct_users)}")
    
    # Simulate the aggregation
    pipeline = [
        {"$match": {"source": source}},
        {"$group": {
            "_id": "$user_id",
            "last_active": {"$max": "$timestamp"},
            "message_count": {"$sum": 1}
        }},
        {"$sort": {"last_active": -1}},
        {"$skip": 0},
        {"$limit": 20}
    ]
    results = list(col.aggregate(pipeline))
    print(f"Aggregation results count: {len(results)}")
    
    for res in results:
        print(f"User: {res['_id']}, Last Active: {res['last_active']}, Count: {res['message_count']}")

except Exception as e:
    print(f"Error: {e}")
