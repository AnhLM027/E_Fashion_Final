package org.example.e_fashion.controller.client;

import lombok.RequiredArgsConstructor;
import org.example.e_fashion.dto.request.ChatMessageRequestDTO;
import org.example.e_fashion.dto.response.ChatMessageResponseDTO;
import org.example.e_fashion.entity.enums.MessageType;
import org.example.e_fashion.entity.enums.SenderType;
import org.example.e_fashion.service.AiChatService;
import org.example.e_fashion.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/ai")
@RequiredArgsConstructor
@Slf4j
public class AiChatController {
    
    private final ObjectMapper objectMapper;

    private final AiChatService aiChatService;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/{sessionId}")
    public Mono<ChatMessageResponseDTO> askAi(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> payload
    ) {
        String userMessage = payload.get("message");
        
        // 1. Save user message
        ChatMessageRequestDTO userReq = new ChatMessageRequestDTO();
        userReq.setSessionId(sessionId);
        userReq.setSenderType(SenderType.USER);
        userReq.setMessageType(MessageType.TEXT);
        userReq.setContent(userMessage);
        chatService.saveMessage(userReq);

        // 2. Get history for context (last 10 messages)
        List<ChatMessageResponseDTO> history = chatService.getMessages(sessionId);
        List<Map<String, String>> aiHistory = new ArrayList<>();
        
        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            ChatMessageResponseDTO msg = history.get(i);
            String role = msg.getSenderType() == SenderType.USER ? "user" : "assistant";
            aiHistory.add(Map.of("role", role, "content", msg.getContent()));
        }
        
        // 3. Call AiChatService (non-streaming for this simple endpoint)
        return aiChatService.streamChat(sessionId, userMessage, aiHistory)
                .collectList()
                .map(list -> {
                    StringBuilder fullText = new StringBuilder();
                    String finalAnswer = null;

                    for (String chunk : list) {
                        try {
                            // Xử lý nếu chunk chứa tiền tố SSE "data: "
                            String json = chunk;
                            if (json.startsWith("data: ")) {
                                json = json.substring(6).trim();
                            }
                            
                            if (json.equals("[DONE]") || json.isEmpty()) continue;

                            JsonNode node = objectMapper.readTree(json);
                            // Event 'token' trả về text
                            if (node.has("text")) {
                                fullText.append(node.get("text").asText());
                            }
                            // Event 'complete' trả về answer cuối cùng
                            if (node.has("answer")) {
                                finalAnswer = node.get("answer").asText();
                            }
                        } catch (IOException e) {
                            log.warn("Failed to parse RAG chunk: {}", chunk);
                        }
                    }
                    return (finalAnswer != null && !finalAnswer.isEmpty()) ? finalAnswer : fullText.toString();
                })
                .map(aiResponse -> {
                    // 4. Save AI Response
                    ChatMessageRequestDTO aiReq = new ChatMessageRequestDTO();
                    aiReq.setSessionId(sessionId);
                    aiReq.setSenderType(SenderType.BOT);
                    aiReq.setMessageType(MessageType.TEXT);
                    aiReq.setContent(aiResponse);
                    
                    ChatMessageResponseDTO savedAiMsg = chatService.saveMessage(aiReq);
                    
                    // 5. Notify via WebSocket so UI updates in real-time if open
                    messagingTemplate.convertAndSend("/topic/session/" + sessionId, savedAiMsg);
                    
                    return savedAiMsg;
                });
    }
}
