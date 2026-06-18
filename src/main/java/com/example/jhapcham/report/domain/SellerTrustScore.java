package com.example.jhapcham.report.domain;

import com.example.jhapcham.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_trust_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerTrustScore {

    @Id
    @Column(name = "seller_id")
    private Long sellerId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "seller_id")
    private User seller;

    @Builder.Default
    @Column(nullable = false)
    private Integer score = 100;

    @Builder.Default
    @Column(name = "fraud_risk_score", nullable = false)
    private Integer fraudRiskScore = 0;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}
