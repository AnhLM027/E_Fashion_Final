import httpx
import os

RAG_API_BASE = "http://localhost:4560/api/v1"
CATALOG_FILE = "efashion_catalog.md"
WORKSPACE_ID = "10"

async def ingest_catalog():
    if not os.path.exists(CATALOG_FILE):
        print(f"Error: {CATALOG_FILE} not found. Run export_data.py first.")
        return

    print(f"Uploading {CATALOG_FILE} to RAG workspace {WORKSPACE_ID}...")
    
    url = f"{RAG_API_BASE}/upload/"
    
    try:
        async with httpx.AsyncClient(timeout=300.0) as client:
            with open(CATALOG_FILE, "rb") as f:
                files = {"files": (CATALOG_FILE, f, "text/markdown")}
                data = {
                    "workspace_id": WORKSPACE_ID
                }
                
                response = await client.post(url, files=files, data=data)
                
            if response.status_code == 200:
                print("Successfully uploaded data to RAG!")
                
                # Step 2: Trigger processing
                print(f"Triggering processing for workspace {WORKSPACE_ID}...")
                process_url = f"{RAG_API_BASE}/upload/process"
                process_resp = await client.post(process_url, params={"workspace_id": WORKSPACE_ID})
                
                if process_resp.status_code == 200:
                    print("Processing started in background.")
                    print(process_resp.json())
                else:
                    print(f"Failed to start processing: {process_resp.status_code}")
                    print(process_resp.text)
            else:
                print(f"Failed to upload data: {response.status_code}")
                print(response.text)
                
    except Exception as e:
        print(f"Error connecting to RAG service: {e}")

if __name__ == "__main__":
    import asyncio
    asyncio.run(ingest_catalog())
