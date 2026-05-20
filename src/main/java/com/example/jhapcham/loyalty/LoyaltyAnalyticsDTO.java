package com.example.jhapcham.loyalty;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class LoyaltyAnalyticsDTO {
    private long totalWallets;
    private long frozenWallets;
    private long suspiciousWallets;
    private Long pointsEarned;
    private Long pointsRedeemed;
    private Long pointsExpired;
    private List<Map<String, Object>> dailyTrend;
}
