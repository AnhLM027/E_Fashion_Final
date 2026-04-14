import numpy as np
from qdrant_client import QdrantClient
from qdrant_client.http import models as rest

def test_qdrant_search():
    # Connect to the new Qdrant test instance on localhost:6336
    client = QdrantClient(host="localhost", port=6336)
    
    collection_name = "test_collection"
    vector_size = 128
    
    print(f"--- Qdrant Diagnostic Script (Target: localhost:6336) ---")
    
    try:
        # 1. Check if collection exists, create if not
        collections = client.get_collections().collections
        exists = any(c.name == collection_name for c in collections)
        
        if not exists:
            print(f"Creating collection '{collection_name}'...")
            client.create_collection(
                collection_name=collection_name,
                vectors_config=rest.VectorParams(size=vector_size, distance=rest.Distance.COSINE),
            )
            print("Collection created.")
        else:
            print(f"Collection '{collection_name}' already exists.")
            
        # 2. Insert some test data
        print("Inserting test data...")
        num_points = 10
        vectors = np.random.rand(num_points, vector_size).tolist()
        client.upsert(
            collection_name=collection_name,
            points=[
                rest.PointStruct(
                    id=i,
                    vector=vectors[i],
                    payload={"color": "blue" if i % 2 == 0 else "red", "rand_val": i}
                ) for i in range(num_points)
            ]
        )
        print(f"Upserted {num_points} points.")

        # 3. Perform a search
        print("Performing vector search...")
        query_vector = np.random.rand(vector_size).tolist()
        search_result = client.query_points(
            collection_name=collection_name,
            query=query_vector,
            limit=5
        ).points
        
        print(f"Search Results:")
        for res in search_result:
            print(f" - ID: {res.id}, Score: {res.score:.4f}, Payload: {res.payload}")

        # 4. Perform a filtered search
        print("\nPerforming filtered search (color=red)...")
        filtered_result = client.query_points(
            collection_name=collection_name,
            query=query_vector,
            query_filter=rest.Filter(
                must=[
                    rest.FieldCondition(
                        key="color",
                        match=rest.MatchValue(value="red"),
                    )
                ]
            ),
            limit=3
        ).points
        
        for res in filtered_result:
            print(f" - ID: {res.id}, Score: {res.score:.4f}, Payload: {res.payload}")

        print("--- Success! ---")
        
    except Exception as e:
        print(f"--- FAILED ---")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    test_qdrant_search()
