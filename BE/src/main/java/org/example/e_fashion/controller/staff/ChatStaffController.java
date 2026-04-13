package org.example.e_fashion.controller.staff;

import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/staff/chat")
@RequiredArgsConstructor
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

        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // Validate image only
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        String uploadDir = fileStorageConfig.getUploadDir() + "/chat";
        File directory = new File(uploadDir);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + "." + extension;

        File dest = new File(directory, fileName);
        file.transferTo(dest);

        // Trả về URL public
        return "http://localhost:2000/uploads/chat/" + fileName;
    }

    @PostMapping("/sessions/{sessionId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String sessionId
    ) {
        chatService.markAsReadForAdmin(sessionId);
        return ResponseEntity.ok().build();
    }
}
