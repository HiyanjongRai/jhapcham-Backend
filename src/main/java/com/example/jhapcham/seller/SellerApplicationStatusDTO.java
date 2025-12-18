package com.example.jhapcham.seller;

import java.time.LocalDateTime;

public record SellerApplicationStatusDTO(
        boolean submitted,
        String status,
        String message,
        String note,
        Long applicationId,
        LocalDateTime submittedAt
) {
}