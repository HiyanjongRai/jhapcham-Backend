package com.example.jhapcham.notification;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationDTO {
    Long id;
    String title;
    String message;
    NotificationType type;
    Long relatedEntityId;
    Boolean isRead;
    LocalDateTime createdAt;
}

