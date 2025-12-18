package com.example.jhapcham.notification;

import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type; // REPORT_ALERT, ORDER_UPDATE, etc.

    private Long relatedEntityId; // Product ID, Report ID, etc.

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
