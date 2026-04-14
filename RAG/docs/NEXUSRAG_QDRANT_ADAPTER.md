# NexusRAG + Qdrant Adapter

Tài liệu này mô tả cách `MBA_RAG` đã tái cấu trúc luồng retrieval để dùng kiến trúc NexusRAG nhưng vẫn giữ Qdrant làm vector store.

## Mục tiêu

- Giữ lại các ưu điểm của NexusRAG:
  - KG + vector + rerank hybrid retrieval
  - Cross-encoder reranking để giảm nhiễu
  - Context format có citations
- Giữ lại Qdrant hiện tại của `MBA_RAG`
- Bảo toàn các tính năng MBA_RAG đang dùng:
  - `KnowledgeGraphService`
  - Lấy ảnh/bảng theo page metadata
  - Compatibility API `RAGOrchestrator`

## Các module mới

### `app/services/deep_retriever.py`

- Core hybrid retriever.
- Chạy song song:
  1. KG query qua `KnowledgeGraphService`
  2. Vector over-fetch qua `VectorStore` (Qdrant)
- Sau đó:
  - `reranker.rerank(...)`
  - tìm ảnh/tables từ SQL theo `document_id` + `page_no`
  - ghép context trả về

### `app/services/nexus_rag_service.py`

- High-level wrapper.
- Cung cấp `query_deep(...)` như NexusRAG.
- Dùng `DeepRetriever` bên dưới và giữ lại cấu trúc truy vấn tương tự NexusRAG.

### `app/services/rag_orchestrator.py`

- Compatibility wrapper cho toàn bộ hệ thống.
- Giữ nguyên các API hiện tại và bây giờ chuyển sang gọi `NexusRAGService.query_deep(...)`.

## Lưu ý về metadata

Qdrant payload hiện tại vẫn dùng:

- `document_id`
- `page_no`
- `heading_path`
- `image_refs` / `image_ids`
- `table_refs` / `table_ids`

DeepRetriever hỗ trợ cả hai dạng `image_refs` và `image_ids` để tăng tính tương thích.

## Tương lai

- Có thể mở rộng `DeepRetriever.query(...)` để hỗ trợ `metadata_filter` / `document_ids` / `mode="vector_only"`.
- Nếu cần thay Qdrant bằng Chroma hoặc Milvus, chỉ cần thay `VectorStore` adapter.
