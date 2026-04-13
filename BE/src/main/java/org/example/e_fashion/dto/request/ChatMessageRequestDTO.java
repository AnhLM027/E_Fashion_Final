package org.example.e_fashion.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.example.e_fashion.entity.enums.MessageType;
import org.example.e_fashion.entity.enums.SenderType;

import java.util.Map;

@Getter
@Setter
public class ChatMessageRequestDTO {

    private String sessionId;
    private SenderType senderType; // USER / AGENT
    private MessageType messageType; // TEXT / IMAGE
    private Map<String, Object> metadata;
    private String content;
}