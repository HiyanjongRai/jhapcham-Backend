package com.example.jhapcham.loyalty;

import com.example.jhapcham.order.Order;
import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_transactions", indexes = {
        @Index(name = "idx_loyalty_tx_customer_created", columnList = "customer_id,created_at"),
        @Index(name = "idx_loyalty_tx_order_type", columnList = "order_id,transaction_type"),
        @Index(name = "idx_loyalty_tx_status", columnList = "status"),
        @Index(name = "idx_loyalty_tx_ref", columnList = "reference_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 40)
    private LoyaltyTransactionType transactionType;

    @Column(nullable = false)
    private Long points;

    @Column(precision = 38, scale = 2)
    private BigDecimal monetaryValue;

    @Column(nullable = false, length = 700)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoyaltyTransactionStatus status;

    @Column(name = "reference_key", nullable = false, length = 180, unique = true)
    private String referenceKey;

    @Column(length = 1000)
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime availableAt;
    private LocalDateTime reversedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
