package com.example.jhapcham.loyalty;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tier_configs", indexes = {
        @Index(name = "idx_tier_configs_tier", columnList = "tier", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TierConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, unique = true)
    private LoyaltyTier tier;

    @Column(nullable = false)
    private Long minLifetimePoints;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal rewardMultiplier;

    @Column(nullable = false, length = 1000)
    private String benefits;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
