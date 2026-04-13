package org.example.e_fashion.repository;

import org.example.e_fashion.entity.ChatMessageEntity;
import org.example.e_fashion.entity.enums.SenderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessageEntity, String> {

    List<ChatMessageEntity> findBySession_IdOrderByCreatedAtAsc(String sessionId);

    @Query("""
    SELECT COUNT(m)
    FROM ChatMessageEntity m
    WHERE m.session.id = :sessionId
      AND m.senderType = 'USER'
      AND m.isRead = false
""")
    Long countUnreadForAdmin(@Param("sessionId") String sessionId);


    @Query("""
    SELECT COUNT(m)
    FROM ChatMessageEntity m
    WHERE m.session.id = :sessionId
      AND m.senderType = 'AGENT'
      AND m.isRead = false
""")
    Long countUnreadForUser(@Param("sessionId") String sessionId);


    @Modifying
    @Query("""
    UPDATE ChatMessageEntity m
    SET m.isRead = true
    WHERE m.session.id = :sessionId
      AND m.senderType = :senderType
      AND m.isRead = false
""")
    int markAsReadBySender(
            @Param("sessionId") String sessionId,
            @Param("senderType") SenderType senderType
    );

    Optional<ChatMessageEntity> findTopBySession_IdOrderByCreatedAtDesc(String sessionId);

    Long countBySessionIdAndSenderTypeAndIsReadFalse(
            String sessionId,
            SenderType senderType
    );
}