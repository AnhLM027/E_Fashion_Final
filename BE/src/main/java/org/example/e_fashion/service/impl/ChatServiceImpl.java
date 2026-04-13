package org.example.e_fashion.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ChatMessageRequestDTO;
import org.example.e_fashion.dto.response.AdminChatSessionResponseDTO;
import org.example.e_fashion.dto.response.ChatMessageResponseDTO;
import org.example.e_fashion.dto.response.ChatSessionResponseDTO;
import org.example.e_fashion.entity.*;
import org.example.e_fashion.entity.enums.MessageType;
import org.example.e_fashion.entity.enums.SenderType;
import org.example.e_fashion.repository.*;
import org.example.e_fashion.service.ChatService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final ChatFeedbackRepository feedbackRepo;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 1️⃣ Tạo hoặc lấy session đang ACTIVE
    @Transactional
    @Override
    public String createOrGetSession(String userId, String guestId) {

        // ===== CASE 1: USER LOGIN =====
        if (userId != null) {

            return sessionRepo
                    .findByUser_IdAndStatus(userId, "ACTIVE")
                    .map(ChatSessionEntity::getId)
                    .orElseGet(() -> {

                        UserEntity user = userRepository.findById(userId)
                                .orElseThrow();

                        ChatSessionEntity session = new ChatSessionEntity();
                        session.setUser(user);
                        session.setStatus("ACTIVE");
                        session.setPlatform("WEB");

                        sessionRepo.save(session);
                        return session.getId();
                    });
        }

        // ===== CASE 2: GUEST =====
        if (guestId != null) {

            return sessionRepo
                    .findByGuestIdAndStatus(guestId, "ACTIVE")
                    .map(ChatSessionEntity::getId)
                    .orElseGet(() -> {

                        ChatSessionEntity session = new ChatSessionEntity();
                        session.setGuestId(guestId);
                        session.setStatus("ACTIVE");
                        session.setPlatform("WEB");

                        sessionRepo.save(session);
                        return session.getId();
                    });
        }

        throw new RuntimeException("userId or guestId must not be null");
    }

    // 2️⃣ Lưu message
    @Transactional
    @Override
    public ChatMessageResponseDTO saveMessage(ChatMessageRequestDTO request) {

        ChatSessionEntity session = sessionRepo
                .findById(request.getSessionId())
                .orElseThrow();

        ChatMessageEntity message = new ChatMessageEntity();
        message.setSession(session);
        message.setSenderType(request.getSenderType());
        message.setMessageType(request.getMessageType());
        message.setContent(request.getContent());
        message.setMetadata(request.getMetadata());
        message.setIsRead(false);

        ChatMessageEntity saved = messageRepo.save(message);

        ChatMessageResponseDTO response = new ChatMessageResponseDTO(
                saved.getId(),
                session.getId(),
                saved.getSenderType(),
                saved.getMessageType(),
                saved.getContent(),
                saved.getMetadata(),
                saved.getIsRead(),
                saved.getCreatedAt()
        );
        System.out.println(response);
        return response;
    }

    @Override
    public List<AdminChatSessionResponseDTO> getActiveSessions() {

        List<ChatSessionEntity> sessions =
                sessionRepo.findByStatusOrderByCreatedAtDesc("ACTIVE");

        return sessions.stream()
                .map(this::buildAdminSessionDTO)
                .toList();
    }

    // 3️⃣ Load lịch sử chat
    @Override
    public List<ChatMessageResponseDTO> getMessages(String sessionId) {

        return messageRepo
                .findBySession_IdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(m -> new ChatMessageResponseDTO(
                        m.getId(),
                        m.getSession().getId(),
                        m.getSenderType(),
                        m.getMessageType(),
                        m.getContent(),
                        m.getMetadata(),
                        m.getIsRead(),
                        m.getCreatedAt()
                ))
                .toList();
    }

    private AdminChatSessionResponseDTO buildAdminSessionDTO(ChatSessionEntity session) {

        ChatMessageEntity lastMessage =
                messageRepo.findTopBySession_IdOrderByCreatedAtDesc(session.getId())
                        .orElse(null);

        Long unread = messageRepo.countUnreadForAdmin(session.getId());

        String userId = null;
        String fullName = null;
        String email = null;

        if (session.getUser() != null) {
            userId = session.getUser().getId();
            fullName = session.getUser().getFullName();
            email = session.getUser().getEmail();
        }

        String lastContent = null;
        MessageType lastType = null;
        LocalDateTime lastTime = null;

        if (lastMessage != null) {
            lastContent = lastMessage.getContent();
            lastType = lastMessage.getMessageType();
            lastTime = lastMessage.getCreatedAt();
        }

        return new AdminChatSessionResponseDTO(
                session.getId(),
                userId,
                fullName,
                email,
                session.getCreatedAt(),
                session.getStatus(),
                unread,
                lastContent,
                lastType,
                lastTime
        );
    }

    private ChatSessionResponseDTO buildUserSessionDTO(ChatSessionEntity session) {

        ChatMessageEntity lastMessage =
                messageRepo.findTopBySession_IdOrderByCreatedAtDesc(session.getId())
                        .orElse(null);

        // 🔥 User unread = AGENT gửi mà chưa đọc
        Long unread = messageRepo.countUnreadForUser(session.getId());

        String lastContent = null;
        MessageType lastType = null;
        LocalDateTime lastTime = null;

        if (lastMessage != null) {
            lastContent = lastMessage.getContent();
            lastType = lastMessage.getMessageType();
            lastTime = lastMessage.getCreatedAt();
        }

        return new ChatSessionResponseDTO(
                session.getId(),
                session.getStatus(),
                unread,
                lastContent,
                lastType,
                lastTime
        );
    }

    @Override
    public ChatSessionResponseDTO getSession(String userId, String guestId) {

        ChatSessionEntity session = null;

        // CASE 1: USER LOGIN
        if (userId != null) {
            session = sessionRepo
                    .findByUser_IdAndStatus(userId, "ACTIVE")
                    .orElse(null);
        }

        // CASE 2: GUEST
        if (session == null && guestId != null) {
            session = sessionRepo
                    .findByGuestIdAndStatus(guestId, "ACTIVE")
                    .orElse(null);
        }

        if (session == null) {
            return null; // hoặc tự create session nếu bạn muốn
        }

        return buildUserSessionDTO(session);
    }

    @Transactional
    @Override
    public void mergeGuestSession(String guestId, String userId) {

        var guestSessionOpt = sessionRepo.findByGuestIdAndStatus(guestId, "ACTIVE");
        if (guestSessionOpt.isEmpty()) return;

        var userSessionOpt = sessionRepo.findByUser_IdAndStatus(userId, "ACTIVE");

        ChatSessionEntity guestSession = guestSessionOpt.get();
        UserEntity user = userRepository.findById(userId).orElseThrow();

        ChatSessionEntity finalSession;

        if (userSessionOpt.isPresent()) {

            // ===== CASE 1: User đã có session ACTIVE =====
            ChatSessionEntity userSession = userSessionOpt.get();

            List<ChatMessageEntity> messages =
                    messageRepo.findBySession_IdOrderByCreatedAtAsc(guestSession.getId());

            for (ChatMessageEntity message : messages) {
                message.setSession(userSession);
            }

            messageRepo.saveAll(messages);
            messageRepo.flush();

            guestSession.setStatus("CLOSED");
            guestSession.setClosedAt(LocalDateTime.now());

            finalSession = userSession;

            // 🔥 SEND CLOSE EVENT FOR GUEST SESSION
            messagingTemplate.convertAndSend(
                    "/topic/admin/session-update",
                    buildAdminSessionDTO(guestSession)
            );

        } else {

            // ===== CASE 2: Upgrade guest thành user =====
            guestSession.setUser(user);
            guestSession.setGuestId(null);

            finalSession = guestSession;
        }

        // ================================
        // 🔥 TẠO SYSTEM MESSAGE
        // ================================
        ChatMessageEntity systemMessage = new ChatMessageEntity();
        systemMessage.setSession(finalSession);
        systemMessage.setSenderType(SenderType.BOT);
        systemMessage.setMessageType(MessageType.SYSTEM);
        systemMessage.setContent("Người dùng đã đăng nhập. Phiên trò chuyện được hợp nhất.");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "MERGE");
        systemMessage.setMetadata(metadata);
        systemMessage.setIsRead(true);

        ChatMessageEntity savedSystemMessage = messageRepo.save(systemMessage);

        // 🔥 SEND REALTIME MESSAGE TO SESSION
        messagingTemplate.convertAndSend(
                "/topic/session/" + finalSession.getId(),
                new ChatMessageResponseDTO(
                        savedSystemMessage.getId(),
                        finalSession.getId(),
                        savedSystemMessage.getSenderType(),
                        savedSystemMessage.getMessageType(),
                        savedSystemMessage.getContent(),
                        null,
                        savedSystemMessage.getIsRead(),
                        savedSystemMessage.getCreatedAt()
                )
        );

        // 🔥 SEND UPDATE EVENT FOR FINAL SESSION
        messagingTemplate.convertAndSend(
                "/topic/admin/session-update",
                buildAdminSessionDTO(finalSession)
        );
    }

    // 5️⃣ Đóng session
    @Transactional
    @Override
    public void closeSession(String sessionId, String summary) {

        ChatSessionEntity session = sessionRepo
                .findById(sessionId)
                .orElseThrow();

        session.setStatus("CLOSED");
        session.setClosedAt(LocalDateTime.now());
        session.setSummary(summary);
    }

    // 6️⃣ Gửi feedback
    @Transactional
    @Override
    public void sendFeedback(String sessionId, Integer rating, String comment) {

        if (feedbackRepo.existsBySession_Id(sessionId)) {
            throw new RuntimeException("Feedback already exists");
        }

        ChatSessionEntity session = sessionRepo
                .findById(sessionId)
                .orElseThrow();

        ChatFeedbackEntity feedback = new ChatFeedbackEntity();
        feedback.setSession(session);
        feedback.setRating(rating);
        feedback.setComment(comment);

        feedbackRepo.save(feedback);
    }

    @Transactional
    @Override
    public void markAsReadForUser(String sessionId) {
        int updated = messageRepo.markAsReadBySender(
                sessionId,
                SenderType.AGENT
        );

        if (updated > 0) {
            messagingTemplate.convertAndSend(
                    "/topic/session/" + sessionId,
                    (Object) Map.of("type", "READ")
            );
        }
    }

    @Transactional
    @Override
    public void markAsReadForAdmin(String sessionId) {
        int updated = messageRepo.markAsReadBySender(
                sessionId,
                SenderType.USER
        );

        if (updated > 0) {
            messagingTemplate.convertAndSend(
                    "/topic/session/" + sessionId,
                    (Object) Map.of("type", "READ")
            );
        }
    }
}