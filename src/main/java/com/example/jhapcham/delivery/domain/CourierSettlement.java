package com.example.jhapcham.delivery.domain;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tracks every COD parcel delivered by a courier.
 * The courier physically holds the collected cash until it is remitted to the Hub Admin.
 * The seller payout is NOT credited until remittedToHub = true.
 */
@Entity
@Table(name = "courier_settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "courier_id", nullable = false)
    private Courier courier;

    @Column(nullable = false, unique = true, length = 60)
    private String trackingId;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal collectedAmount;

    @Builder.Default
    @Column(nullable = false)
    private boolean remittedToHub = false;

    private LocalDateTime remittedAt;
    private String remittanceNote;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
