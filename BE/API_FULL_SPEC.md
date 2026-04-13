# 📖 E_Fashion API Specification (Final Refactored)

Tài liệu này tổng hợp toàn bộ các API Backend của hệ thống E_Fashion sau khi đã phân tách hoàn toàn mã nguồn dựa trên vai trò Staff (Vận hành) và Admin (Quản trị cấp cao).

---

## 📂 1. Quản lý Xác thực (`/api/auth`)
*Dành cho tất cả người dùng (Public).*

- **Đăng ký**: `POST /api/auth/register`
- **Đăng nhập**: `POST /api/auth/login` (Trả về Cookie `accessToken`, `refreshToken`).
- **Quên/Reset mật khẩu**: `POST /api/auth/forgot-password`, `POST /api/auth/reset-password`

---

## 📂 2. Luồng VẬN HÀNH (`/api/staff/**`)
*Dành cho cả **STAFF** và **ADMIN**. Tập trung vào hỗ trợ khách hàng và cập nhật kho.*

### 2.1 Quản lý Đọc (Read-Only)
- **Thương hiệu**: `GET /api/staff/brands` (Lấy danh sách cho việc đăng sản phẩm).
- **Danh mục**: `GET /api/staff/categories`, `GET /api/staff/categories/tree`.
- **Khuyến mãi**: `GET /api/staff/coupons`.

### 2.2 Quản lý Sản phẩm (Operational)
- **Sản phẩm (Cha)**: `GET /api/staff/products` (Filters: `categoryId`, `brandId`, `status`).
- **Thao tác**: `POST/PUT /api/staff/products` (Tạo/Sửa), `DELETE /api/staff/products/{id}` (Chỉ ẩn sản phẩm - Soft Delete).
- **Biến thể & Tồn kho**: `POST/PUT/DELETE /api/staff/product-variants/**`, `.../product-variant-sizes/**`.
- **Ảnh biến thể**: `POST/PATCH/DELETE /api/staff/product-variants/{variantId}/images/**`.

### 2.3 Quản lý Đơn hàng & Chat (Fulfillment)
- **Đơn hàng**: `GET/PUT /api/staff/orders/**` (Cập nhật trạng thái đơn, xem lịch sử, chi tiết).
- **Trò chuyện**: `GET/POST /api/staff/chat/**` (Phản hồi khách hàng qua WebSocket/REST).

---

## 📂 3. Luồng QUẢN TRỊ CẤP CAO (`/api/admin/**`)
*Dành riêng cho **ADMIN**. Thay đổi cấu trúc và hệ thống.*

### 3.1 Quản lý cấu trúc (Write/Delete)
- **Thương hiệu**: `POST/PUT/DELETE /api/admin/brands`
- **Danh mục**: `POST/PUT/DELETE /api/admin/categories`
- **Khuyến mãi**: `POST/PUT/DELETE /api/admin/coupons`
- **Sản phẩm**: `DELETE /api/admin/products/{id}/hard` (Xóa vĩnh viễn), `PATCH /api/admin/products/{id}/restore` (Khôi phục).

### 3.2 Quản lý Hệ thống
- **Người dùng**: `GET/PUT /api/admin/users/**` (Phân quyền, Khóa tài khoản).
- **Báo cáo tài chính**: `GET /api/admin/dashboard` (Dữ liệu doanh thu nội bộ).

---

## 📂 4. Luồng KHÁCH HÀNG / CÔNG KHAI
- **🛒 Customer (`/api/customer/**`)**: Giỏ hàng, Hồ sơ, Đặt hàng.
- **🌍 Public (`/api/products/**`)**: Xem sản phẩm, Đánh giá công khai.

---

## 📂 5. Thông tin Kỹ Thuật
- **CORS**: Cho phép `localhost:5173`.
- **WebSocket**: `ws://localhost:2000/ws` (Dành cho Chat Admin).
