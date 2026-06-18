package com.example.jhapcham.message.api;

import com.example.jhapcham.message.application.MessageService;
import com.example.jhapcham.message.dto.MessageDTO;
import com.example.jhapcham.message.dto.SendMessageRequest;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.user.domain.User;
import com.example.jhapcham.user.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/messages")
@RequiredArgsConstructor
public class AdminMessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;

    /** GET /api/admin/messages/inbox — all messages received by the admin */
    @GetMapping("/inbox")
    public ResponseEntity<?> getAdminInbox(Authentication authentication) {
        User admin = resolveAdmin(authentication);
        if (admin == null) return ResponseEntity.status(401).body("Unauthorized");
        List<MessageDTO> msgs = messageService.getMessagesForUser(admin.getId());
        return ResponseEntity.ok(msgs);
    }

    /** GET /api/admin/messages/sent — all messages sent by the admin */
    @GetMapping("/sent")
    public ResponseEntity<?> getAdminSent(Authentication authentication) {
        User admin = resolveAdmin(authentication);
        if (admin == null) return ResponseEntity.status(401).body("Unauthorized");
        List<MessageDTO> msgs = messageService.getSentMessages(admin.getId());
        return ResponseEntity.ok(msgs);
    }

    /** GET /api/admin/messages/conversation/{userId} — full thread with a user */
    @GetMapping("/conversation/{userId}")
    public ResponseEntity<?> getConversation(@PathVariable Long userId,
                                             Authentication authentication) {
        User admin = resolveAdmin(authentication);
        if (admin == null) return ResponseEntity.status(401).body("Unauthorized");
        List<MessageDTO> msgs = messageService.getConversation(admin.getId(), userId);
        return ResponseEntity.ok(msgs);
    }

    /** POST /api/admin/messages/send — send a message to a single user */
    @PostMapping("/send")
    public ResponseEntity<?> sendToUser(@RequestBody SendMessageRequest request,
                                        Authentication authentication) {
        User admin = resolveAdmin(authentication);
        if (admin == null) return ResponseEntity.status(401).body("Unauthorized");
        try {
            MessageDTO sent = messageService.sendMessage(admin.getId(), request);
            return ResponseEntity.ok(sent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * POST /api/admin/messages/broadcast
     * Body: { "content": "..." }
     * Sends the message from the admin to every non-admin user.
     */
    @PostMapping("/broadcast")
    public ResponseEntity<?> broadcastToAllUsers(@RequestBody Map<String, String> body,
                                                 Authentication authentication) {
        User admin = resolveAdmin(authentication);
        if (admin == null) return ResponseEntity.status(401).body("Unauthorized");

        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body("Message content is required");
        }

        List<User> allUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .toList();

        int successCount = 0;
        for (User user : allUsers) {
            try {
                SendMessageRequest req = new SendMessageRequest();
                req.setReceiverId(user.getId());
                req.setContent(content);
                messageService.sendMessage(admin.getId(), req);
                successCount++;
            } catch (Exception ignored) {
                // Skip users that can't receive (e.g. blocked); continue
            }
        }

        return ResponseEntity.ok(Map.of(
                "sent", successCount,
                "total", allUsers.size(),
                "message", "Broadcast sent to " + successCount + " users"
        ));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private User resolveAdmin(Authentication authentication) {
        if (authentication == null || authentication.getName().equals("anonymousUser")) return null;
        String principal = authentication.getName();
        return userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .filter(u -> u.getRole() == Role.ADMIN)
                .orElse(null);
    }
}
