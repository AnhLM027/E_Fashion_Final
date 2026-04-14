# Tài liệu chi tiết các Dịch vụ (Services) - MBA_RAG

Tài liệu này tập trung vào logic nghiệp vụ và các tham số kỹ thuật của từng Service trong hệ thống.

---

## 1. RAG Orchestrator (`rag_orchestrator.py`)

Bộ điều phối trung tâm cho luồng truy vấn Hybrid.

### 📋 Phương thức chính
- **`retrieve_context(query, top_k=5, prefetch_k=20)`**:
    - **Input**: Câu hỏi thô của người dùng.
    - **Logic**: 
        1. Gọi `Embedder` để chuyển câu hỏi sang Vector.
        2. Tìm kiếm tại Qdrant lấy `prefetch_k` (thường là 20) kết quả thô.
        3. Đưa 20 kết quả này vào bộ **Cross-Encoder Reranker** để tính toán độ tương quan thực tế.
        4. Trả ra `top_k` kết quả có điểm rerank cao nhất.
    - **Output**: List các object gồm `document`, `metadata`, `rerank_score`.

- **`generate_context_string(results)`**:
    - **Logic**: Chuyển đổi List kết quả thành một chuỗi văn bản có gắn kèm trích dẫn (Citations) theo định dạng chuẩn để đưa vào LLM.

### 🧩 NexusRAG Adapter
- `app/services/nexus_rag_service.py`: Wrapper để giữ cấu trúc truy vấn như NexusRAG nhưng dùng lại Qdrant.
- `app/services/deep_retriever.py`: Core retrieval logic, thực hiện:
    1. parallel KG query + Qdrant over-fetch
    2. cross-encoder reranking
    3. metadata/image/table lookup
    4. cấu trúc context cho LLM
- `app/services/rag_orchestrator.py`: compatibility layer vẫn giữ API hiện tại và chuyển sang gọi `NexusRAGService.query_deep()`.

---

## 2. Chatbot Ami Service (`chatbot_ami.py`)

Quản lý linh hồn và logic hội thoại của hệ thống.

### 📋 Chế độ hoạt động (Modes)
- **`ami_study`**: Trợ lý học tập nhiệt tình, giải thích dễ hiểu, xưng "tớ - cậu".
- **`ami_debate_round`**: Tranh biện sắc sảo, chỉ đặt một câu hỏi phản biện duy nhất, bám sát tài liệu.
- **`ami_debate_score`**: Giám khảo nghiêm khắc, trả về định dạng JSON thuần để Frontend xử lý UI điểm số.

### 📋 Phương thức chính
- **`chat(user_id, session_id, subject_name, query, context, history)`**:
    - **Logic**: Xây dựng System Prompt dựa trên `mode`, đóng gói `context` và gửi tới OpenAI API. Sau đó tự động lưu kết quả vào MongoDB.

---

## 3. Vector Store (`vector_store.py`)

Lớp giao tiếp giữa ứng dụng và cơ sở dữ liệu Vector (Qdrant).

### 📋 Đặc điểm
- **Namespace Isolation**: Mỗi môn học (workspace) được lưu trong 1 `collection` riêng biệt tại Qdrant (ví dụ: `kb_1`).
- **UUID Deterministic**: Tự động sinh ID duy nhất cho mỗi chunk để tránh bị lặp dữ liệu nếu nạp lại file cũ.

---

## 4. Ingestion Pipeline (Loader & Chunker)

Module xử lý đầu vào tài liệu.

### 📂 `document_loader.py`
- Sử dụng công nghệ **Docling** của IBM.
- Có khả năng bóc tách siêu văn bản: Nhận diện bảng biểu, công thức toán học và sơ đồ.
- Đầu ra là một đối tượng `ParsedDocument` chứa toàn bộ cấu trúc file.

### 📂 `chunker.py`
- Chia nhỏ văn bản dựa trên ngữ cảnh (Paragraph/Section) thay vì cắt tịnh tiến theo số từ.
- Đảm bảo mỗi chunk giữ được ý nghĩa trọn vẹn của đoạn văn.

---

## 5. Intelligence Layer (Embedder & Reranker)

Bộ đôi tạo nên sức mạnh trí tuệ của hệ thống.

### 🧠 `embedder.py`
- Mặc định sử dụng model **BGE-M3** hoặc **OpenAI text-embedding-3-large**.
- Chuyên dùng để biến văn bản thành vector 1024/3072 chiều.

### 🧠 `reranker.py`
- Sử dụng **BAAI/bge-reranker-v2-m3**.
- Khác với tìm kiếm Vector thông thường (chỉ tính khoảng cách), Reranker thực sự "đọc" cả câu hỏi và văn bản để chấm điểm liên quan, giúp loại bỏ các kết quả nhiễu.

---

## 🔗 Luồng dữ liệu (Data Flow)
`User Query` ➔ `Orchestrator` ➔ `Embedder` ➔ `Qdrant` ➔ `Reranker` ➔ `ChatbotAmi` ➔ `OpenAI` ➔ `MongoDB` ➔ `User Response`
