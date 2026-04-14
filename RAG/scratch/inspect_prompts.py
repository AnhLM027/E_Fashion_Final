import lightrag.prompt
print("Available LightRAG prompt keys:")
for key in lightrag.prompt.PROMPTS.keys():
    print(f"- {key}")
