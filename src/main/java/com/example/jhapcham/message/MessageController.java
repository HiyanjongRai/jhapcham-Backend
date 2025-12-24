package com.example.jhapcham.message;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageRequest request,
            Authentication authentication) {
        try {
            String principal = authentication.getName();
            User user = userRepository.findByUsername(principal)
                    .or(() -> userRepository.findByEmail(principal))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(messageService.sendMessage(user.getId(), request));
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.toString();
            return ResponseEntity.badRequest().body("Error: " + errorMsg);
        }
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<MessageDTO>> getInbox(Authentication authentication) {
        String principal = authentication.getName();
        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(messageService.getMessagesForUser(user.getId()));
    }

    @GetMapping("/sent")
    public ResponseEntity<List<MessageDTO>> getSent(Authentication authentication) {
        String principal = authentication.getName();
        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(messageService.getSentMessages(user.getId()));
    }

    @GetMapping("/conversation/{otherUserId}")
    public ResponseEntity<List<MessageDTO>> getConversation(@PathVariable Long otherUserId,
            Authentication authentication) {
        String principal = authentication.getName();
        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(messageService.getConversation(user.getId(), otherUserId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        String principal = authentication.getName();
        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(messageService.getUnreadCount(user.getId()));
    }

    @PostMapping("/mark-read/{senderId}")
    public ResponseEntity<Void> markAsRead(@PathVariable Long senderId, Authentication authentication) {
        String principal = authentication.getName();
        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));
        messageService.markAsRead(user.getId(), senderId);
        return ResponseEntity.ok().build();
    }
}
