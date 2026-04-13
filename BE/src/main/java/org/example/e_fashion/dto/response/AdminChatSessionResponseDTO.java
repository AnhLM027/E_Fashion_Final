package org.example.e_fashion.dto.response;

import lombok.Getter;
import lombok.AllArgsConstructor;
import org.example.e_fashion.entity.enums.MessageType;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminChatSessionResponseDTO {

    private String id;
    private String userId;
    private String fullName;
    private String email;
    private LocalDateTime createdAt;
    private String status;
    private Long unreadCount;
    private String lastMessage;
    private MessageType lastMessageType;
    private LocalDateTime lastTime;
}
