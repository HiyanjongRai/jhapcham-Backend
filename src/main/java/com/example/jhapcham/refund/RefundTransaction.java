package com.example.jhapcham.refund;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_transactions",
        indexes = {
                @Index(name = "idx_refund_transactions_request", columnList = "refund_request_id"),
                @Index(name = "idx_refund_transactions_gateway_status", columnList = "gateway,status"),
                @Index(name = "idx_refund_transactions_provider_reference", columnList = "provider_refund_reference")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_refund_transaction_idempotency", columnNames = {"idempotency_key"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_request_id", nullable = false)
    @JsonIgnore
    private RefundRequest refundRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundGateway gateway;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundTransactionStatus status;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 160)
    private String idempotencyKey;

    private String providerPaymentReference;
    private String providerRefundReference;

    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @Column(nullable = false)
    private int attemptCount;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime confirmedAt;
}
