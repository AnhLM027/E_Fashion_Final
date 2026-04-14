package org.example.e_fashion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiChatService {

    private final WebClient webClient;

    @Value("${nexusrag.api.base:http://localhost:8080/nexus/api/v1}")
    private String nexusRagApiBase;

    @Value("${nexusrag.workspace.id:10}")
    private String workspaceId;

    public AiChatService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Gửi tin nhắn tới AI Bot (NexusRAG) và nhận phản hồi streaming
     */
    public Flux<String> streamChat(String message, List<Map<String, String>> history) {
        String url = String.format("%s/rag/chat/%s/stream", nexusRagApiBase, workspaceId);

        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("history", history != null ? history : List.of());
        body.put("mode", "hybrid");
        body.put("top_k", 8);

        log.info("Calling NexusRAG Stream API: {}", url);

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnError(e -> log.error("Error streaming from NexusRAG (URL: {}): {}", url, e.getMessage()));
    }
}
