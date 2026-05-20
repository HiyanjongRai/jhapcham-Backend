package com.example.jhapcham.loyalty;

import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_wallets", indexes = {
        @Index(name = "idx_loyalty_wallet_user", columnList = "customer_id", unique = true),
        @Index(name = "idx_loyalty_wallet_tier", columnList = "tier"),
        @Index(name = "idx_loyalty_wallet_frozen", columnList = "frozen,suspicious")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyWallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private User customer;

    @Column(nullable = false)
    @Builder.Default
    private Long totalPointsEarned = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long availablePoints = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long redeemedPoints = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long expiredPoints = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long lifetimePoints = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long pendingPoints = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LoyaltyTier tier = LoyaltyTier.BRONZE;

    @Column(nullable = false)
    @Builder.Default
    private boolean frozen = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean suspicious = false;

    @Column(length = 500)
    private String fraudReason;

    private LocalDateTime lastEarnedAt;
    private LocalDateTime lastRedeemedAt;
    private LocalDateTime tierUpdatedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (tierUpdatedAt == null) {
            tierUpdatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
