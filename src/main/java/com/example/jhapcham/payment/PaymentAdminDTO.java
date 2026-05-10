package com.example.jhapcham.payment;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAdminDTO {
    private Long paymentId;
    private Long orderId;
    private String customerName;
    private String customerEmail;
    private String orderStatus;

    private String method;
    private String state;
    private BigDecimal amount;

    private String transactionUuid;
    private String providerReferenceId;

    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}

