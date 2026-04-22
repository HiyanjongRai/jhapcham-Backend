package com.example.jhapcham.loyalty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyPointsDTO {
    private Long id;
    private Long totalPoints;
    private Long redeemedPoints;
    private Long availablePoints;
    private LocalDateTime lastRedeemedAt;
    private String tier;
    private String nextTier;
    private Long pointsToNextTier;
}
