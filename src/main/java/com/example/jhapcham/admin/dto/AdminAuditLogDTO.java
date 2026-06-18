package com.example.jhapcham.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminAuditLogDTO {
    private Long id;
    private Long actorId;
    private String actorUsername;
    private String action;
    private String targetType;
    private Long targetId;
    private String summary;
    private String metadata;
    private LocalDateTime createdAt;
}
