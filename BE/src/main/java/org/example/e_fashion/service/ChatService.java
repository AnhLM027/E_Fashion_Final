package org.example.e_fashion.service;

import org.example.e_fashion.dto.request.ChatMessageRequestDTO;
import org.example.e_fashion.dto.response.AdminChatSessionResponseDTO;
import org.example.e_fashion.dto.response.ChatMessageResponseDTO;
import org.example.e_fashion.dto.response.ChatSessionResponseDTO;
import org.example.e_fashion.entity.ChatSessionEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChatService {

    // 1️⃣ Tạo hoặc lấy session đang ACTIVE
    @Transactional
    String createOrGetSession(String userId, String guestId);

    // 2️⃣ Lưu message
    @Transactional
    ChatMessageResponseDTO saveMessage(ChatMessageRequestDTO request);

    // 3️⃣ Load lịch sử chat
    List<ChatMessageResponseDTO> getMessages(String sessionId);

    // 4️⃣ Admin lấy session ACTIVE
    List<AdminChatSessionResponseDTO> getActiveSessions();

    ChatSessionResponseDTO getSession(String userId, String guestId);

    @Transactional
    void mergeGuestSession(String guestId, String userId);

    // 5️⃣ Đóng session
    @Transactional
    void closeSession(String sessionId, String summary);

    // 6️⃣ Gửi feedback
    @Transactional
    void sendFeedback(String sessionId, Integer rating, String comment);

    @Transactional
    void markAsReadForUser(String sessionId);

    @Transactional
    void markAsReadForAdmin(String sessionId);
}
