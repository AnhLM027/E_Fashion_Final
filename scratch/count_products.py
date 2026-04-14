import re

sql_file = '/home/naver/Desktop/AnhLM027/E_Fashion/Dump20260413.sql'
with open(sql_file, 'r', encoding='utf-8') as f:
    for line in f:
        if line.startswith('INSERT INTO `products` VALUES'):
            # This line contains many product entries like ('id', 'name', ...)
            # We want to count how many such entries there are.
            # Entries are separated by ),(
            # But the content itself might have parentheses.
            # However, SQL dumps usually follow a specific pattern.
            
            # Let's count occurrences of ',1,\'2026-03-14' which seems to be at the end of each row
            matches = re.findall(r"\',1,\'2026-03-14", line)
            print(f"Number of products found: {len(matches)}")
            
            # Let's also extract the names
            names = re.findall(r"\',\'([^\']+)\',\'[^\']+\',\'### TÊN SẢN PHẨM", line)
            print(f"Extracted {len(names)} names")
            for name in names[:10]:
                print(f" - {name}")
            break
