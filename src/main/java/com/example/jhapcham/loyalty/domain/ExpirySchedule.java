package com.example.jhapcham.loyalty.domain;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
import com.example.jhapcham.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_expiry_schedule", indexes = {
        @Index(name = "idx_expiry_customer", columnList = "customer_id"),
        @Index(name = "idx_expiry_due", columnList = "expires_at,expired,notified")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpirySchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @OneToOne(optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private LoyaltyTransaction transaction;

    @Column(nullable = false)
    private Long pointsRemaining;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean notified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean expired = false;
}
