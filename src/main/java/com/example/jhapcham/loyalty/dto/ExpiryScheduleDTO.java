package com.example.jhapcham.loyalty.dto;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
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
