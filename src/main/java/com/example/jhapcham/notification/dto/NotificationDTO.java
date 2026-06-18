package com.example.jhapcham.notification.dto;


import com.example.jhapcham.notification.application.*;
import com.example.jhapcham.notification.domain.*;
import com.example.jhapcham.notification.dto.*;
import com.example.jhapcham.notification.persistence.*;
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

