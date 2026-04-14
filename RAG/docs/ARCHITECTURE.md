# MBA_RAG Architecture

Đây là tài liệu kiến trúc cho hệ thống `MBA_RAG`, mô tả chi tiết cách hệ thống kết hợp những gì tốt nhất từ "NexusRAG" (RAG pipeline tiên tiến) và "MBA_API" (RAG có tính ổn định, chịu tải chuyên biệt cho GD).

## 1. Mục tiêu kiến trúc (Hybrid Stack)
- **Database Quan hệ (Postgres)**: Lưu metadata, danh sách workspace, trạng thái chunking, quản lý thông tin học sinh (có tính ACID mạnh).
- **Cơ sở dữ liệu Vector (Qdrant)**: Tìm kiếm siêu nhanh, tương thích tốt với các text-embeddings, hỗ trợ filtering (lọc Payload). Thay thế ChromaDB truyền thống của Nexus để ưu tiên hiệu năng.
- **Graph Database (Neo4j)**: Phục vụ GraphRAG, lưu trữ các Entity/Relationship nếu muốn bot có góc nhìn đồ thị tri thức môn học.
- **Tài liệu phi cấu trúc (MongoDB)**: Lưu trữ lịch sử chat, do ngữ cảnh chat Ami (debate, study) luôn đa dạng.

## 2. Các Modules Cốt lõi

### 2.1 Lớp Ingestion (`app/services/document_loader.py` & `chunker.py`)
Tiếp nhận file PDF/DOCX từ Router. 
- Sử dụng **Docling** (từ Nexus) thay vì `SimpleDirectoryReader` (LlamaIndex) để trích xuất cả công thức toán, bảng và hình ảnh từ tài liệu.
- Sử dụng thuật toán `chunk_dedup.py` nhằm loại bỏ chunk trùng lặp, đảm bảo Qdrant không bị "quá tải" những thông tin vô giá trị.
- Nhúng (Embed) và lưu kết quả vào `vector_store.py` (sau lưng là client Qdrant).

### 2.2 Lớp Retrieval (`app/services/rag_orchestrator.py`)
Chịu trách nhiệm cho việc **"Search"**:
1. Đọc query từ sinh viên.
2. Embed Query thông qua `app/services/embedder.py`.
3. Tìm kiếm Vector (top_k=20) tại Qdrant.
4. Chấm điểm lại (Reranking) sử dụng bộ Reranker từ Nexus (ví dụ: BGE-Reranker) để chọn ra Top 5 chunk thực sự sát nghĩa nhất.
5. Xâu chuỗi (Format string) thành Context dồn cho LLM.

> Lưu ý: Luồng truy vấn đã được refactor theo mô hình NexusRAG adapter. `rag_orchestrator.py` hiện là compatibility wrapper, còn phần core retrieval được thực hiện trong `app/services/deep_retriever.py` và `app/services/nexus_rag_service.py`.

### 2.3 Lớp Tương tác LLM (`app/services/chatbot_ami.py`)
Thay thế `load_chat.py` của MBA_API cũ nhưng giữ nguyên "Tâm hồn" (System Prompt):
- Quản lý các mode của nhân vật **Ami**: `ami_study`, `ami_debate_round`, `ami_debate_score`.
- Router yêu cầu gọi tới cấu trúc này, lấy Context từ Orchestrator, sau đó feed vào OpenAI, và đợi Response.
- Chat history được log real-time xuống MongoDB thông qua module `app/db/mongodb.py`.

## 3. Deployment và Scaling
- Các thư viện cốt lõi nằm ở `requirements.txt`.
- Cấu hình qua `.env` (gián tiếp load bằng Pydantic `app/core/config.py`).
- Để Scale:
  - Tách worker process cho Ingestion và Querying (ví dụ Dùng Celery cho file upload lớn).
  - Khởi chạy nhiều node Qdrant / MongoDB dạng replicas cluster cho môi trường Production.
