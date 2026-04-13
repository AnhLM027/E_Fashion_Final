package org.example.e_fashion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_feedbacks")
@Getter
@Setter
public class ChatFeedbackEntity {

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", unique = true)
    private ChatSessionEntity session;

    private Integer rating;
    private String comment;

    @Column(insertable = false)
    private LocalDateTime createdAt;
}