package com.example.jhapcham.loyalty.dto;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
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
