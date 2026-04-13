package org.example.e_fashion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.e_fashion.entity.enums.MessageType;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatSessionResponseDTO {

    private String sessionId;
    private String status;

    private Long unreadCount;     // số tin ADMIN gửi mà user chưa đọc

    private String lastContent;
    private MessageType lastMessageType;
    private LocalDateTime lastTime;
}