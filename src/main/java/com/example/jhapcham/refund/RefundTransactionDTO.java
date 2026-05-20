package com.example.jhapcham.refund;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RefundTransactionDTO {
    private Long id;
    private RefundGateway gateway;
    private RefundTransactionStatus status;
    private BigDecimal amount;
    private String providerRefundReference;
    private String failureReason;
    private int attemptCount;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
}
