import pymysql
import os
import json
import requests
from pathlib import Path

# Database configuration (using host port 3307 as mapped in docker-compose.yml)
DB_CONFIG = {
    "host": "localhost",
    "port": 3307,
    "user": "root",
    "password": "root_password",
    "database": "e_fashion",
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor,
}

# NexusRAG Configuration
NEXUSRAG_API_BASE = "http://localhost:8080/nexus/api/v1"

def get_or_create_workspace():
    print("Checking for existing workspaces in NexusRAG...")
    try:
        # 1. List workspaces
        resp = requests.get(f"{NEXUSRAG_API_BASE}/workspaces")
        if resp.status_code == 200:
            workspaces = resp.json()
            if workspaces:
                ws_id = workspaces[0]["id"]
                print(f"Using existing workspace: {workspaces[0]['name']} (ID: {ws_id})")
                return ws_id
        
        # 2. If no workspaces, create one
        print("No workspace found. Creating 'E-Fashion Catalog' workspace...")
        payload = {
            "name": "E-Fashion Catalog",
            "description": "Product catalog for E-Fashion store recommendation bot",
            "kg_language": "Vietnamese",
            "kg_entity_types": ["Product", "Brand", "Category", "Financial_Metric"]
        }
        resp = requests.post(f"{NEXUSRAG_API_BASE}/workspaces", json=payload)
        if resp.status_code == 201:
            ws_id = resp.json()["id"]
            print(f"Created workspace: E-Fashion Catalog (ID: {ws_id})")
            return ws_id
        else:
            print(f"Failed to create workspace: {resp.text}")
            return None
    except Exception as e:
        print(f"Error communicating with NexusRAG Workspace API: {e}")
        return None

def extract_data():
    print("Connecting to E-Fashion database...")
    try:
        connection = pymysql.connect(**DB_CONFIG)
    except Exception as e:
        print(f"Error connecting to database: {e}")
        print("Tip: Make sure the e_fashion_db container is running and port 3307 is accessible.")
        return None

    try:
        with connection.cursor() as cursor:
            # 1. Fetch Categories
            cursor.execute("SELECT id, name FROM categories")
            categories = {row['id']: row['name'] for row in cursor.fetchall()}

            # 2. Fetch Brands
            cursor.execute("SELECT id, name FROM brands")
            brands = {row['id']: row['name'] for row in cursor.fetchall()}

            # 3. Fetch Products
            cursor.execute("""
                SELECT p.id, p.name, p.description, p.category_id, p.brand_id, p.slug
                FROM products p
                WHERE p.deleted_at IS NULL AND p.is_active = 1
            """)
            products = cursor.fetchall()

            # 4. Fetch Variants, Colors, and Sizes
            # We'll build a detailed catalog
            from datetime import datetime
            catalog_content = f"Exported at: {datetime.now().isoformat()}\n\n"

            for p in products:
                brand_name = brands.get(p['brand_id'], "Unknown Brand")
                cat_name = categories.get(p['category_id'], "Unknown Category")
                
                catalog_content += f"## {p['name']}\n"
                catalog_content += f"- **ID**: `{p['id']}`\n"
                catalog_content += f"- **Brand**: {brand_name}\n"
                catalog_content += f"- **Category**: {cat_name}\n"
                catalog_content += f"- **Description**: {p['description']}\n"
                
                # Fetch variants for this product
                cursor.execute("""
                    SELECT pv.id, c.name as color_name, c.code as color_code
                    FROM product_variants pv
                    JOIN colors c ON pv.color_id = c.id
                    WHERE pv.product_id = %s AND pv.deleted_at IS NULL AND pv.is_active = 1
                """, (p['id'],))
                variants = cursor.fetchall()
                
                if variants:
                    catalog_content += "### Variants\n"
                    for v in variants:
                        catalog_content += f"#### Color: {v['color_name']} ({v['color_code']})\n"
                        
                        # Fetch sizes and prices for this variant
                        cursor.execute("""
                            SELECT size_name, sku, original_price, sale_price, stock
                            FROM product_variant_sizes
                            WHERE variant_id = %s
                        """, (v['id'],))
                        sizes = cursor.fetchall()
                        
                        if sizes:
                            catalog_content += "| Size | SKU | Original Price | Sale Price | Stock |\n"
                            catalog_content += "|------|-----|----------------|------------|-------|\n"
                            for s in sizes:
                                catalog_content += f"| {s['size_name']} | {s['sku']} | {s['original_price']:,}₫ | **{s['sale_price']:,}₫** | {s['stock']} |\n"
                        catalog_content += "\n"
                
            # 5. Fetch Order Summary (Financial Metrics)
            cursor.execute("""
                SELECT 
                    COUNT(*) as total_orders,
                    SUM(final_price) as total_revenue,
                    AVG(final_price) as avg_order_value,
                    status
                FROM orders
                GROUP BY status
            """)
            order_stats = cursor.fetchall()

            catalog_content += "## Financial Metrics & Order Summary\n"
            if order_stats:
                catalog_content += "| Status | Count | Revenue | Avg Value |\n"
                catalog_content += "|--------|-------|---------|-----------|\n"
                for stat in order_stats:
                    catalog_content += f"| {stat['status']} | {stat['total_orders']} | {stat['total_revenue'] or 0:,.0f}₫ | {stat['avg_order_value'] or 0:,.0f}₫ |\n"
            else:
                catalog_content += "No order data available.\n"
            catalog_content += "\n"

            # 6. Fetch Recent Orders
            cursor.execute("""
                SELECT o.id, o.receiver_name, o.total_price, o.status, o.created_at, u.full_name as customer_name
                FROM orders o
                LEFT JOIN users u ON o.user_id = u.id
                ORDER BY o.created_at DESC
                LIMIT 50
            """)
            recent_orders = cursor.fetchall()
            
            catalog_content += "### Recent Transactions\n"
            if recent_orders:
                catalog_content += "| Order ID | Customer | Amount | Status | Date |\n"
                catalog_content += "|----------|----------|--------|--------|------|\n"
                for o in recent_orders:
                    customer = o['customer_name'] or o['receiver_name']
                    date_str = o['created_at'].strftime("%Y-%m-%d %H:%M") if hasattr(o['created_at'], 'strftime') else str(o['created_at'])
                    catalog_content += f"| `{o['id'][:8]}...` | {customer} | {o['total_price']:,}₫ | {o['status']} | {date_str} |\n"
            else:
                catalog_content += "No recent transactions found.\n"
            catalog_content += "\n---\n\n"

            return catalog_content

    finally:
        connection.close()

def upload_to_nexusrag(content):
    workspace_id = get_or_create_workspace()
    if not workspace_id:
        print("Aborting upload: No workspace available.")
        return

    file_path = "efashion_catalog.md"
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)
    
    print(f"Generated {file_path}. Uploading to Workspace ID: {workspace_id}...")
    
    try:
        # 1. Upload
        with open(file_path, "rb") as f:
            files = {"file": (file_path, f, "text/markdown")}
            response = requests.post(f"{NEXUSRAG_API_BASE}/documents/upload/{workspace_id}", files=files)
        
        if response.status_code != 200:
            print(f"Failed to upload: {response.text}")
            return
        
        data = response.json()
        doc_id = data["id"]
        print(f"Successfully uploaded. Document ID: {doc_id}")
        
        # 2. Process
        print("Starting indexing process...")
        process_resp = requests.post(f"{NEXUSRAG_API_BASE}/rag/process/{doc_id}")
        if process_resp.status_code == 200:
            print("Indexing started in background. You can check the status in NexusRAG UI.")
        else:
            print(f"Failed to start indexing: {process_resp.text}")
            
    except Exception as e:
        print(f"Error communicating with NexusRAG: {e}")
        print("Tip: Make sure NexusRAG backend is running on http://localhost:8080")

if __name__ == "__main__":
    # Check if pymysql is installed, if not try to install it
    try:
        import pymysql
    except ImportError:
        print("Installing pymysql...")
        os.system("pip install pymysql")
    
    catalog = extract_data()
    if catalog:
        upload_to_nexusrag(catalog)
