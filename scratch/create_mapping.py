import json
import os

data_dir = '/home/naver/Desktop/AnhLM027/E_Fashion/FE/data'
files = ['ao.json', 'quan.json', 'giay-dep-nam.json', 'vi-da.json']

mapping = {}
for f in files:
    path = os.path.join(data_dir, f)
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8') as j:
            data = json.load(j)
            for item in data['data']:
                mapping[item['title']] = item['link']
    else:
        print(f"Warning: {path} not found")

print(f"Total mappings: {len(mapping)}")
with open('/home/naver/Desktop/AnhLM027/E_Fashion/scratch/name_to_link.json', 'w', encoding='utf-8') as out:
    json.dump(mapping, out, ensure_ascii=False, indent=4)
