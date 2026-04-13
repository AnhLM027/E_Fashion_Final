package org.example.e_fashion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.e_fashion.entity.enums.MessageType;
import org.example.e_fashion.entity.enums.SenderType;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ChatMessageResponseDTO {

    private String id;
    private String sessionId;
    private SenderType senderType;
    private MessageType messageType;
    private String content;
    private Map<String, Object> metadata;
    private Boolean isRead;
    private LocalDateTime createdAt;
}