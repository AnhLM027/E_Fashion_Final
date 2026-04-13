package org.example.e_fashion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.e_fashion.converter.JsonConverter;
import org.example.e_fashion.entity.enums.MessageType;
import org.example.e_fashion.entity.enums.SenderType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
public class ChatMessageEntity {

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSessionEntity session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderType senderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Convert(converter = JsonConverter.class)
    @Column(columnDefinition = "JSON")
    private Map<String, Object> metadata;

    private Boolean isRead = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}