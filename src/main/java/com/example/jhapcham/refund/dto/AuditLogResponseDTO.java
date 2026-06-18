package com.example.jhapcham.refund.dto;

import com.example.jhapcham.refund.domain.RefundStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponseDTO {
    private Long id;
    private RefundStatus oldStatus;
    private RefundStatus newStatus;
    private Long actorId;
    private String actorRole;
    private String notes;
    private LocalDateTime createdAt;
}
