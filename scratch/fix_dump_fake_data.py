import re
import os

def generate_fake_description(name):
    # Determine basic category from name to make the fake data look slightly better
    category = "Sản phẩm"
    if "Áo" in name:
        category = "Áo thời trang"
    elif "Quần" in name:
        category = "Quần thời trang"
    elif "Giày" in name or "Dép" in name:
        category = "Giày dép cao cấp"
    elif "Ví" in name or "Bóp" in name or "Thắt lưng" in name:
        category = "Phụ kiện đồ da"
        
    desc = f"### TÊN SẢN PHẨM\\n{name}\\n### MÔ TẢ\\nĐây là mô tả chi tiết cho sản phẩm {name}. Sản phẩm {category.lower()} mang phong cách hiện đại, trẻ trung và thanh lịch, là lựa chọn hoàn hảo cho quý ông công sở và những buổi dạo phố.\\n### ĐẶC ĐIỂM NỔI BẬT\\n- Thiết kế tinh tế, tỉ mỉ trong từng đường kim mũi chỉ.\\n- Chất liệu cao cấp, thoáng mát và bền bỉ trong quá trình sử dụng.\\n- Form dáng chuẩn, tôn dáng người mặc và mang lại sự tự tin.\\n### HƯỚNG DẪN BẢO QUẢN\\n- Giặt máy ở chế độ nhẹ hoặc giặt tay để bền màu.\\n- Không sử dụng chất tẩy mạnh.\\n- Phơi trong bóng râm, tránh ánh nắng trực tiếp.\\n### LƯU Ý\\n- Hình ảnh chỉ mang tính chất minh họa. Màu sắc thực tế có thể chênh lệch nhẹ do điều kiện ánh sáng."
    return desc

sql_input = '/home/naver/Desktop/AnhLM027/E_Fashion/Dump20260413.sql'
sql_output = '/home/naver/Desktop/AnhLM027/E_Fashion/Dump20260413_Updated.sql'

with open(sql_input, 'r', encoding='utf-8') as f:
    lines = f.readlines()

updated_lines = []
for line in lines:
    if line.startswith('INSERT INTO `products` VALUES'):
        # The line format is: INSERT INTO `products` VALUES ('id1','name1','slug1','desc1',...),('id2','name2','slug2','desc2',...);
        prefix = 'INSERT INTO `products` VALUES '
        content = line[len(prefix):].strip()
        if content.endswith(';'):
            content = content[:-1]
        
        # We need to split the tuples carefully.
        # tuples are separated by ),(
        # and each tuple has fields separated by ,
        
        # Using a more robust regex to find all tuples
        # Pattern: ( 'id', 'name', 'slug', 'description', ... )
        # Fields are single-quoted strings or NULL or numbers.
        
        # This regex matches a single tuple and captures its fields
        # fields: id, name, slug, description, thumbnail, category_id, brand_id, is_active, created_at, updated_at, deleted_at
        pattern = re.compile(r"\(('[^']*'),('[^']*'),('[^']*'),('[^']*'),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*),([^,]*)\)")
        
        matches = pattern.findall(content)
        new_tuples = []
        for m in matches:
            id_val, name_val, slug_val, old_desc, thumb, cat, brand, active, created, updated, deleted = m
            
            # Remove quotes for processing
            clean_name = name_val[1:-1]
            # Generate new fake description
            new_desc_text = generate_fake_description(clean_name)
            # Surround with single quotes and escape internal single quotes if any
            new_desc = "'" + new_desc_text.replace("'", "''") + "'"
            
            new_tuple = f"({id_val},{name_val},{slug_val},{new_desc},{thumb},{cat},{brand},{active},{created},{updated},{deleted})"
            new_tuples.append(new_tuple)
        
        new_line = prefix + ",".join(new_tuples) + ";\n"
        updated_lines.append(new_line)
        print(f"Updated {len(new_tuples)} product descriptions in the SQL line.")
    else:
        updated_lines.append(line)

with open(sql_output, 'w', encoding='utf-8') as f:
    f.writelines(updated_lines)

print(f"Successfully wrote updated SQL dump to: {sql_output}")
