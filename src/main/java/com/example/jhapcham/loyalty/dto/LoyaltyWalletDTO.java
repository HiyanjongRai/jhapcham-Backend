package com.example.jhapcham.loyalty.dto;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LoyaltyWalletDTO {
    private Long id;
    private Long customerId;
    private Long totalPointsEarned;
    private Long availablePoints;
    private Long redeemedPoints;
    private Long expiredPoints;
    private Long lifetimePoints;
    private Long pendingPoints;
    private LoyaltyTier tier;
    private LoyaltyTier nextTier;
    private Long pointsToNextTier;
    private int tierProgressPercent;
    private boolean frozen;
    private boolean suspicious;
    private String benefits;
    private LocalDateTime lastEarnedAt;
    private LocalDateTime lastRedeemedAt;
    private List<LoyaltyTransactionDTO> recentTransactions;
    private List<ExpiryScheduleDTO> expiringPoints;

    public Long getPoints() {
        return availablePoints;
    }

    public Long getTotalPoints() {
        return totalPointsEarned;
    }
}
