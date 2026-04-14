package org.example.e_fashion.controller.client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.e_fashion.config.FileStorageConfig;
import org.example.e_fashion.dto.response.ChatMessageResponseDTO;
import org.example.e_fashion.dto.response.ChatSessionResponseDTO;
import org.example.e_fashion.entity.UserEntity;
import org.example.e_fashion.service.ChatService;
import org.example.e_fashion.utils.ExtractUserUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ExtractUserUtils extractUserUtils;
    private final FileStorageConfig fileStorageConfig;

    // USER tạo hoặc lấy session ACTIVE
    @PostMapping("/session")
    public String createSession(
            HttpServletRequest request,
            @RequestParam(required = false) String guestId
    ) {

        String userId = null;

        try {
            userId = extractUserUtils.extract(request).getId();
        } catch (Exception ignored) {
            // Không có JWT → có thể là guest
        }

        if (userId == null && guestId == null) {
            throw new RuntimeException("guestId is required for unauthenticated user");
        }

        return chatService.createOrGetSession(userId, guestId);
    }

    @GetMapping("/session")
    public ResponseEntity<ChatSessionResponseDTO> getSession(
            HttpServletRequest request,
            @RequestParam(required = false) String guestId
    ) {

        UserEntity user = null;

        try {
            user = extractUserUtils.extract(request);
        } catch (Exception ignored) {
            // Không có JWT → guest
        }

        ChatSessionResponseDTO response =
                chatService.getSession(
                        user != null ? user.getId() : null,
                        guestId
                );

        return ResponseEntity.ok(response);
    }

    // USER load lịch sử chat
    @GetMapping("/{sessionId}/messages")
    public List<ChatMessageResponseDTO> getMessages(
            @PathVariable String sessionId) {
        return chatService.getMessages(sessionId);
    }

    @PostMapping("/merge")
    public ResponseEntity<Void> mergeGuestSession(
            @RequestParam String guestId,
            HttpServletRequest request
    ) {
        String userId = null;

        try {
            userId = extractUserUtils.extract(request).getId();
        } catch (Exception ignored) {
            // Không có JWT → có thể là guest
        }
        chatService.mergeGuestSession(guestId, userId);
        return ResponseEntity.ok().build();
    }

    // USER gửi feedback sau khi chat xong
    @PostMapping("/{sessionId}/feedback")
    public void sendFeedback(
            @PathVariable String sessionId,
            @RequestParam Integer rating,
            @RequestParam String comment) {
        chatService.sendFeedback(sessionId, rating, comment);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadChatFile(@RequestParam("file") MultipartFile file)
            throws IOException {
        log.info("Starting file upload (Client): originalFilename={}", file.getOriginalFilename());

        if (file.isEmpty()) {
            log.error("Upload failed: File is empty");
            throw new RuntimeException("File is empty");
        }

        // Validate image only
        String contentType = file.getContentType();
        log.info("File content type: {}", contentType);
        if (contentType == null || !contentType.startsWith("image/")) {
            log.warn("Upload failed: Only image files are allowed. Current type: {}", contentType);
            throw new RuntimeException("Only image files are allowed");
        }

        String uploadDirStr = fileStorageConfig.getUploadDir() + "/chat";
        Path uploadPath = Paths.get(uploadDirStr).toAbsolutePath().normalize();
        File directory = uploadPath.toFile();

        if (!directory.exists()) {
            log.info("Creating directory for chat uploads: {}", uploadPath);
            directory.mkdirs();
        }

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + "." + extension;

        Path destPath = uploadPath.resolve(fileName);
        log.info("Saving file to: {}", destPath);
        
        try {
            Files.copy(file.getInputStream(), destPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File uploaded successfully. Relative path: /style/uploads/chat/{}", fileName);
        } catch (IOException e) {
            log.error("Failed to save file {}: {}", fileName, e.getMessage());
            throw e;
        }

        // Trả về URL relative
        return "/style/uploads/chat/" + fileName;
    }

    @PostMapping("/sessions/{sessionId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String sessionId
    ) {
        chatService.markAsReadForUser(sessionId);
        return ResponseEntity.ok().build();
    }
}