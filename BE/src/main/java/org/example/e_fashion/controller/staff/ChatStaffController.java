package org.example.e_fashion.controller.staff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.e_fashion.config.FileStorageConfig;
import org.example.e_fashion.dto.response.AdminChatSessionResponseDTO;
import org.example.e_fashion.dto.response.ChatMessageResponseDTO;
import org.example.e_fashion.service.ChatService;
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
@RequestMapping("/api/staff/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatStaffController {

    private final ChatService chatService;
    private final FileStorageConfig fileStorageConfig;

    // Lấy session ACTIVE
    @GetMapping("/sessions")
    public List<AdminChatSessionResponseDTO> getActiveSessions() {
        return chatService.getActiveSessions();
    }

    // Load message của 1 session
    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessageResponseDTO> getMessages(
            @PathVariable String sessionId) {
        return chatService.getMessages(sessionId);
    }

    // Close session
    @PostMapping("/sessions/{sessionId}/close")
    public void closeSession(@PathVariable String sessionId) {
        chatService.closeSession(sessionId, "Closed by staff");
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadChatFile(@RequestParam("file") MultipartFile file)
            throws IOException {
        log.info("Starting file upload: originalFilename={}", file.getOriginalFilename());

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
        chatService.markAsReadForAdmin(sessionId);
        return ResponseEntity.ok().build();
    }
}
