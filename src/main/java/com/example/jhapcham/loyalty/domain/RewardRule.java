package com.example.jhapcham.loyalty.domain;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_rules", indexes = {
        @Index(name = "idx_reward_rules_type_active", columnList = "rule_type,active"),
        @Index(name = "idx_reward_rules_category", columnList = "category"),
        @Index(name = "idx_reward_rules_seller", columnList = "seller_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 40)
    private RewardRuleType ruleType;

    @Column(precision = 10, scale = 4, nullable = false)
    private BigDecimal rewardRate;

    @Column(precision = 10, scale = 4)
    private BigDecimal multiplier;

    @Column(length = 1000)
    private String category;

    private Long sellerId;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 100;

    private LocalDateTime startsAt;
    private LocalDateTime endsAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
