package com.example.jhapcham.loyalty;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExpiryScheduleDTO {
    private Long id;
    private Long pointsRemaining;
    private LocalDateTime expiresAt;
    private boolean notified;
}
