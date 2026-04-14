import re
import json

sql_file = '/home/naver/Desktop/AnhLM027/E_Fashion/Dump20260413.sql'
names_in_sql = []
with open(sql_file, 'r', encoding='utf-8') as f:
    for line in f:
        if line.startswith('INSERT INTO `products` VALUES'):
            # Each entry is like ('id','Name','Slug','Description',...)
            # The pattern is: ('uuid','Name','slug','description'
            # Let's split by ),( but it's tricky.
            # Use regex to find all names.
            # Names are the 2nd field.
            # Example: ('0027e283-670d-4402-a18e-c5da919fa14c','Quần Kaki Nam Aristino Regular Fit AKK0100S0','quan-kaki-nam-aristino-regular-fit-akk0100s0',
            matches = re.findall(r"\('[a-f0-9-]+','([^']+)','", line)
            names_in_sql.extend(matches)

print(f"Total names in SQL: {len(names_in_sql)}")
with open('/home/naver/Desktop/AnhLM027/E_Fashion/scratch/sql_names.json', 'w', encoding='utf-8') as out:
    json.dump(names_in_sql, out, ensure_ascii=False, indent=4)
