package org.example.e_fashion.repository;

import org.example.e_fashion.entity.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository
        extends JpaRepository<ChatSessionEntity, String> {

    Optional<ChatSessionEntity> findByUser_IdAndStatus(
            String userId,
            String status
    );

    Optional<ChatSessionEntity> findByGuestIdAndStatus(String guestId, String status);

    List<ChatSessionEntity> findByStatusOrderByCreatedAtDesc(String status);
}