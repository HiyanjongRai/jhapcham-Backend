package com.example.jhapcham.loyalty;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LoyaltyTransactionDTO {
    private Long id;
    private Long customerId;
    private Long orderId;
    private LoyaltyTransactionType transactionType;
    private Long points;
    private BigDecimal monetaryValue;
    private String description;
    private LocalDateTime createdAt;
    private LoyaltyTransactionStatus status;
}
