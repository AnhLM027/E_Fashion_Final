import mysql.connector
import os
from dotenv import load_dotenv

# Load environment variables from BE/.env if exists
load_dotenv("../../BE/.env")

# Database Configuration
DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT_LOCAL", "3307")), # Using mapped port
    "user": os.getenv("DB_USERNAME", "root"),
    "password": os.getenv("DB_PASSWORD", "root_password"),
    "database": os.getenv("DB_NAME", "e_fashion")
}

def export_to_markdown(output_file="efashion_catalog.md"):
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor(dictionary=True)

        print(f"Connected to database: {DB_CONFIG['database']}")

        # 1. Fetch Categories and Brands for overview
        cursor.execute("SELECT name FROM categories")
        categories = [row['name'] for row in cursor.fetchall()]

        cursor.execute("SELECT name FROM brands")
        brands = [row['name'] for row in cursor.fetchall()]

        # 2. Fetch Detailed Product Info
        query = """
        SELECT 
            p.id as product_id,
            p.name as product_name, 
            p.description, 
            c.name as category_name, 
            b.name as brand_name,
            col.name as color_name,
            s.size_name as size,
            s.sale_price as price,
            s.stock as inventory
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        LEFT JOIN brands b ON p.brand_id = b.id
        LEFT JOIN product_variants v ON v.product_id = p.id
        LEFT JOIN colors col ON v.color_id = col.id
        LEFT JOIN product_variant_sizes s ON s.variant_id = v.id
        WHERE p.is_active = 1
        ORDER BY p.name, col.name, s.size_name
        """
        cursor.execute(query)
        rows = cursor.fetchall()

        # 3. Process Data into Markdown
        md_content = "# 📚 E-Fashion Product Catalog\n\n"
        md_content += "Chào mừng bạn đến với danh mục sản phẩm của E-Fashion. Dưới đây là thông tin chi tiết về các sản phẩm, thương hiệu và ngành hàng chúng tôi đang cung cấp.\n\n"
        
        md_content += "## 🏷️ Ngành hàng & Thương hiệu\n"
        md_content += f"- **Ngành hàng:** {', '.join(categories)}\n"
        md_content += f"- **Thương hiệu:** {', '.join(brands)}\n\n"

        md_content += "## 🛍️ Danh sách sản phẩm chi tiết\n\n"

        current_product_id = None
        for row in rows:
            if row['product_id'] != current_product_id:
                current_product_id = row['product_id']
                md_content += f"### {row['product_name']}\n"
                md_content += f"- **Thương hiệu:** {row['brand_name']}\n"
                md_content += f"- **Danh mục:** {row['category_name']}\n"
                md_content += f"- **Mô tả:** {row['description']}\n"
                md_content += "- **Các phân loại:**\n"
            
            if row['color_name'] or row['size']:
                color_str = f"Màu {row['color_name']}" if row['color_name'] else ""
                size_str = f"Size {row['size']}" if row['size'] else ""
                variant_str = f"{color_str} {size_str}".strip()
                price_str = f"{row['price']:,} VNĐ" if row['price'] else "Liên hệ"
                stock_str = f"(Còn hàng: {row['inventory']})" if row['inventory'] is not None else ""
                
                md_content += f"  - {variant_str}: **{price_str}** {stock_str}\n"

        # 4. Save to File
        with open(output_file, "w", encoding="utf-8") as f:
            f.write(md_content)

        print(f"Success! Exported to {output_file}")

    except Exception as e:
        print(f"Error: {e}")
    finally:
        if 'conn' in locals() and conn.is_connected():
            conn.close()

if __name__ == "__main__":
    # Ensure dependencies
    # os.system("pip install mysql-connector-python python-dotenv")
    export_to_markdown()
