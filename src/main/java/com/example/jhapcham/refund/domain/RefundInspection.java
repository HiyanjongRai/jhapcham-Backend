package com.example.jhapcham.refund.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refund_inspection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id", nullable = false)
    private Refund refund;

    @Column(name = "physical_damage", nullable = false)
    private boolean physicalDamage;

    @Column(name = "water_damage", nullable = false)
    private boolean waterDamage;

    @Column(name = "missing_parts", nullable = false)
    private boolean missingParts;

    @Column(name = "burn_damage", nullable = false)
    private boolean burnDamage;

    @Column(name = "tampering", nullable = false)
    private boolean tampering;

    @Column(name = "packaging_intact", nullable = false)
    private boolean packagingIntact;

    @Column(name = "product_matches", nullable = false)
    private boolean productMatches;

    @Column(name = "severity_score", nullable = false)
    private int severityScore;

    @Column(name = "inspector_notes", columnDefinition = "TEXT")
    private String inspectorNotes;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private InspectionVerdict verdict;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
