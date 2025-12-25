package com.example.jhapcham.notification;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createNotification(User user, String title, String message, NotificationType type,
            Long relatedEntityId) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .relatedEntityId(relatedEntityId)
                .build();
        notificationRepository.save(Objects.requireNonNull(notification, "Notification cannot be null"));
    }

    public List<Notification> getUserNotifications(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(Objects.requireNonNull(notificationId, "Notification ID cannot be null"))
                .ifPresent(n -> {
                    n.setIsRead(true);
                    notificationRepository.save(n);
                });
    }

    @Transactional
    public void notifyAllSellers(String title, String message, Long relatedEntityId) {
        List<User> sellers = userRepository.findByRole(com.example.jhapcham.user.model.Role.SELLER);
        for (User seller : sellers) {
            createNotification(seller, title, message, NotificationType.SELLER_ALERT, relatedEntityId);
        }
    }

    @Transactional
    public void notifyAllCustomers(String title, String message, Long relatedEntityId) {
        List<User> customers = userRepository.findByRole(com.example.jhapcham.user.model.Role.CUSTOMER);
        for (User customer : customers) {
            createNotification(customer, title, message, NotificationType.SYSTEM_ALERT, relatedEntityId);
        }
    }
}
