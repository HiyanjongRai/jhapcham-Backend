package com.example.jhapcham.refund;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RefundTimelineDTO {
    private RefundStatus fromStatus;
    private RefundStatus toStatus;
    private RefundActorType actorType;
    private Long actorId;
    private String actorName;
    private String note;
    private LocalDateTime createdAt;
}
