# 📚 E_Fashion Comprehensive API Documentation (Physical Role-Split)

Tài liệu này chi tiết cấu trúc Request/Response cho từng vai trò trong hệ thống E_Fashion.

---

## 👨‍💻 I. VAI TRÒ: NHÂN VIÊN (STAFF/ADMIN) - Tiền tố: `/api/staff/`

### 1. Quản lý Sản phẩm & Biến thể (Operational)
- **Danh sách sản phẩm**: `GET /api/staff/products?brandId=...&status=...` (Lấy sản phẩm đang có trong kho).
- **Thêm sản phẩm**: `POST /api/staff/products` (Tạo sản phẩm cha).
- **Sửa sản phẩm**: `PUT /api/staff/products/{id}`.
- **Ẩn sản phẩm**: `DELETE /api/staff/products/{id}` (Soft Delete).
- **Xem Danh mục/Thương hiệu**: `GET /api/staff/categories`, `GET /api/staff/brands`. (Chỉ xem).

### 2. Quản lý Biến thể hàng hóa
- **Thêm phân loại (Size/Màu)**: `POST /api/staff/product-variants`.
- **Cập nhật tồn kho**: `PUT /api/staff/product-variant-sizes/{id}` (Số lượng, giá bán).

### 3. Vận hành Đơn hàng (Fulfillment)
- **Thay đổi trạng thái**: `PUT /api/staff/orders/{orderId}/status` (Body: `{newStatus: "SHIPPING", note: "..."}`).
- **Xem chi tiết**: `GET /api/staff/orders/{orderId}`.

### 4. Chăm sóc khách hàng (Chat)
- **Tiếp nhận chat**: `GET /api/staff/chat/sessions` (Danh sách phiên chat đang chờ).
- **Lược sử tin nhắn**: `GET /api/staff/chat/sessions/{sessionId}/messages`.

---

## 👑 II. VAI TRÒ: QUẢN TRỊ VIÊN (ADMIN) - Tiền tố: `/api/admin/`

### 1. Cấu hình cấu trúc (Structure Management)
- **Thương hiệu**: `POST/PUT/DELETE /api/admin/brands` (Chỉ Admin mới có quyền thêm thương hiệu mới).
- **Danh mục**: `POST/PUT/DELETE /api/admin/categories` (Chùa Admin mới cấu trúc phân cấp danh mục).
- **Mã giảm giá**: `POST/PUT/DELETE /api/admin/coupons` (Duyệt chiến dịch marketing).

### 2. Xử lý sự cố Dữ liệu
- **Khôi phục sản phẩm**: `PATCH /api/admin/products/{id}/restore`.
- **Xóa vĩnh viễn**: `DELETE /api/admin/products/{id}/hard`.

### 3. Quản trị Tài khoản & Thống kê
- **Quản lý Users**: `GET/PUT /api/admin/users/{userId}/role` (Đổi phân quyền thành STAFF hoặc CUSTOMER).
- **Khóa tài khoản**: `PUT /api/admin/users/{userId}/status?active=false`.
- **Báo cáo doanh thu**: `GET /api/admin/dashboard`.

---

## 🛒 III. VAI TRÒ: KHÁCH HÀNG (CUSTOMER) - Tiền tố: `/api/customer/`

- **Giỏ hàng**: `GET/POST /api/customer/carts`.
- **Thanh toán**: `POST /api/customer/orders`.
- **Hồ sơ**: `GET /api/customer/profile`.

---

## 🌍 IV. CÔNG KHAI (PUBLIC / CLIENT) - Tiền tố: `/api/products/`
- **Tất cả các API Xem công khai**: `/api/products`, `/api/products/slug/{slug}`, `/api/categories/tree`, `/api/brands`. (Không yêu cầu đăng nhập).

---

## 💡 Lưu ý bảo mật:
- Các API dưới tiền tố `/api/admin/**` sẽ bị server trả về **403 Forbidden** nếu User có Role là `STAFF` hoặc `CUSTOMER`.
- Nhân viên vận hành luôn truy cập qua cổng `/api/staff/**`.
