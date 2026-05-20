package com.example.jhapcham.loyalty;

import com.example.jhapcham.order.Order;
import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "redemption_history", indexes = {
        @Index(name = "idx_redemption_customer_created", columnList = "customer_id,created_at"),
        @Index(name = "idx_redemption_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedemptionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long pointsRedeemed;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false)
    @Builder.Default
    private boolean restored = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime restoredAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
