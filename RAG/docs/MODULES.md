# Tài liệu chi tiết các Module - MBA_RAG

Tài liệu này giải thích chi tiết chức năng và cách thức hoạt động của từng file trong hệ thống.

---

## 1. Lớp API (Routes) - `app/api/routes/`

### 📂 `upload.py`
- **Chức năng**: Cổng tiếp nhận tài liệu học thuật (PDF, DOCX, TXT).
- **Key Logic**:
    - `upload_files`: Nhận file, lưu tạm vào disk và đẩy vào xử lý ngầm.
    - `process_file_background`: Chạy pipeline gồm: Parse (Docling) ➔ Chunking ➔ Embedding ➔ Indexing vào Qdrant.
- **Tương tác**: Gọi `document_loader`, `chunker`, `embedder` và `vector_store`.

### 📂 `chat.py`
- **Chức năng**: Xử lý hỏi đáp RAG tiêu chuẩn và giải thích đáp án.
- **Key Logic**:
    - `chat_endpoint`: Đầu não cho chat thường. Gọi `orchestrator` để lấy ngữ cảnh trước khi hỏi LLM thông qua `ChatbotAmiService`.
    - `quiz_explanation`: Nhận list câu hỏi quiz và trả về lời giải thích chi tiết dựa trên tài liệu.
- **Tương tác**: Phối hợp giữa `RAGOrchestrator` và `ChatbotAmiService`.

### 📂 `debate.py`
- **Chức năng**: Quản lý tính năng tranh biện học thuật (Debate).
- **Key Logic**:
    - `/round`: Ami đặt câu hỏi phản biện dựa trên ý của sinh viên.
    - `/score`: Giám khảo Ami chấm điểm và trả về JSON cấu trúc (score, rubric, feedback).
- **Tương tác**: Sử dụng `Ami` prompts đặc biệt.

### 📂 `history.py`
- **Chức năng**: Quản trị dữ liệu lịch sử và bảng xếp hạng.
- **Key Logic**: Trích xuất dữ liệu từ MongoDB cho Frontend hiển thị.

---

## 2. Lớp Dịch vụ (Services) - `app/services/`

### ⚙️ `rag_orchestrator.py`
- **Chức năng**: Điều phối luồng tìm kiếm (Retrieval Pipeline).
- **Quy trình**: Nhận Query ➔ Tìm Qdrant (Top 20) ➔ Gọi **Reranker** chấm điểm lại ➔ Trả về Top 5 sát nhất.
- **Ưu điểm**: Đảm bảo độ chính xác cao hơn hẳn so với tìm kiếm vector thông thường nhờ lớp Rerank.

### ⚙️ `chatbot_ami.py`
- **Chức năng**: Quản lý Persona (nhân vật) Ami và giao tiếp OpenAI.
- **Tính năng**: Chứa các "System Prompt" phức tạp của Ami (Study, Debate, Score). Tự động đóng gói Context vào Prompt.

### ⚙️ `vector_store.py`
- **Chức năng**: Lớp trừu tượng (Abstraction) cho Vector Database.
- **Đặc điểm**: Cung cấp các hàm `add_documents`, `query` theo chuẩn NexusRAG nhưng thực hiện logic trên **Qdrant**. Dễ dàng thay đổi DB khác sau này mà không hỏng logic hệ thống.

### ⚙️ `document_loader.py` & `chunker.py`
- **Chức năng**: Chế biến dữ liệu thô.
- **Docling**: Trích xuất cấu trúc văn bản thông minh (bảng, công thức).
- **Chunker**: Chia nhỏ văn bản và khử trùng lặp (Deduplication).

---

## 3. Lớp Dữ liệu (Database) - `app/db/` & `app/core/`

### 💾 `qdrant.py`
- Cung cấp kết nối Singleton tới Qdrant.
- Có hàm `health_check` để giám sát trạng thái DB.

### 💾 `mongodb.py`
- Xử lý các tác vụ CRUD (Create, Read, Update, Delete) cho lịch sử chat.
- Implement logic Aggregation để tạo **Leaderboard** (Bảng xếp hạng).

### 💾 `config.py`
- Quản lý toàn bộ cấu hình hệ thống bằng `pydantic-settings`.
- Tự động load từ file `.env`.

---

## 📝 Hướng dẫn Mở rộng & Bảo trì

1. **Muốn thêm DB khác?**: Chỉnh sửa file `app/services/vector_store.py`, giữ nguyên tên các hàm.
2. **Muốn đổi Persona (nhân vật AI)?**: Thêm template prompt vào `app/services/chatbot_ami.py`.
3. **Muốn nâng cấp Parser?**: Cập nhật config trong `app/services/document_loader.py`.
