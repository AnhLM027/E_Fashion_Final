package org.example.e_fashion.controller.client;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ChatMessageRequestDTO;
import org.example.e_fashion.dto.response.ChatMessageResponseDTO;
import org.example.e_fashion.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageRequestDTO request) {

        System.out.println("Received: " + request.getContent());

        ChatMessageResponseDTO saved =
                chatService.saveMessage(request);

        messagingTemplate.convertAndSend(
                "/topic/session/" + request.getSessionId(),
                saved
        );

        messagingTemplate.convertAndSend(
                "/topic/admin/messages",
                saved
        );
    }
}