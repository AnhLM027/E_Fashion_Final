from pathlib import Path
from app.services.document_loader import load_txt_file, load_markdown_file
import os

def test_page_count():
    # Test short text
    short_path = Path("short_test.txt")
    short_path.write_text("Hello world", encoding="utf-8")
    doc = load_txt_file(short_path)
    print(f"Short text (11 chars): {doc.page_count} page(s)")
    os.remove(short_path)

    # Test long text
    long_path = Path("long_test.txt")
    content = "A" * 7000 # should be 3 pages (7000 / 3000 = 2.33 -> 3)
    long_path.write_text(content, encoding="utf-8")
    doc = load_txt_file(long_path)
    print(f"Long text (7000 chars): {doc.page_count} page(s)")
    os.remove(long_path)

    # Test markdown
    md_path = Path("test.md")
    md_path.write_text("B" * 3001, encoding="utf-8") # should be 2 pages
    doc = load_markdown_file(md_path)
    print(f"Markdown (3001 chars): {doc.page_count} page(s)")
    os.remove(md_path)

if __name__ == "__main__":
    test_page_count()
