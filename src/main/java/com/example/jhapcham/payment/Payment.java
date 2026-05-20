package com.example.jhapcham.payment;

import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_order", columnList = "order_id", unique = true),
        @Index(name = "idx_payments_transaction_uuid", columnList = "transaction_uuid", unique = true),
        @Index(name = "idx_payments_state_updated", columnList = "state,updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @JsonIgnore
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentState state;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal amount;

    // Provider-specific identifiers (e.g., eSewa transaction_uuid/ref_id)
    @Column(unique = true)
    private String transactionUuid;

    private String providerReferenceId;

    private String failureReason;

    private LocalDateTime initiatedAt;

    private LocalDateTime completedAt;

    private LocalDateTime refundedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
