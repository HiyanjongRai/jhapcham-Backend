package com.example.jhapcham.notification;

import com.example.jhapcham.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        List<Notification> notifications = notificationService.getUserNotifications(user.getEmail());
        return ResponseEntity.ok(
                notifications.stream()
                        .map(n -> NotificationDTO.builder()
                                .id(n.getId())
                                .title(n.getTitle())
                                .message(n.getMessage())
                                .type(n.getType())
                                .relatedEntityId(n.getRelatedEntityId())
                                .isRead(n.getIsRead())
                                .createdAt(n.getCreatedAt())
                                .build())
                        .collect(Collectors.toList()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        long count = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user).size();
        return ResponseEntity.ok((long) count);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        notificationService.markAsRead(id, currentUserService.requireUser(authentication));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/clear")
    public ResponseEntity<Void> clearAll(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<Void> deleteAll(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        notificationService.deleteAllForUser(user);
        return ResponseEntity.ok().build();
    }
}
