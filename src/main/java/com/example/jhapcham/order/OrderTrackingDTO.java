package com.example.jhapcham.order;

import java.time.LocalDateTime;

public record OrderTrackingDTO(
        Long id,
        String stage,
        String message,
        String branch,
        LocalDateTime updateTime,
        Long orderId
) {}
