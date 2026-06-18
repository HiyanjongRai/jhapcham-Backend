package com.example.jhapcham.loyalty.domain;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
import com.example.jhapcham.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Builder.Default
    private Long totalPoints = 0L;

    @Builder.Default
    private Long redeemedPoints = 0L;

    @Builder.Default
    private Long availablePoints = 0L;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastRedeemedAt;

    public void addPoints(Long points) {
        if (points == null || points <= 0) {
            return;
        }
        this.totalPoints += points;
        this.availablePoints += points;
    }

    public void redeemPoints(Long points) {
        if (points == null || points <= 0) {
            throw new RuntimeException("Points to redeem must be greater than zero");
        }
        if (points > this.availablePoints) {
            throw new RuntimeException("Insufficient points for redemption");
        }
        this.availablePoints -= points;
        this.redeemedPoints += points;
        this.lastRedeemedAt = LocalDateTime.now();
    }
}
