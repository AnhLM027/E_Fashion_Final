package org.example.e_fashion.repository;

import org.example.e_fashion.entity.ChatFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface    ChatFeedbackRepository
        extends JpaRepository<ChatFeedbackEntity, String> {

    boolean existsBySession_Id(String sessionId);
}