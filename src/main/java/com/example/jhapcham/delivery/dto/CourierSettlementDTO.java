package com.example.jhapcham.delivery.dto;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CourierSettlementDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SettlementView {
        private Long settlementId;
        private String trackingId;
        private BigDecimal collectedAmount;
        private boolean remittedToHub;
        private LocalDateTime remittedAt;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemittanceRequest {
        private String remittanceNote;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourierCashSummary {
        private Long courierId;
        private String courierName;
        private BigDecimal totalPendingCash;
        private int pendingDeliveryCount;
    }
}
