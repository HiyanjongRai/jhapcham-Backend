package com.example.jhapcham.seller;

import com.example.jhapcham.user.model.Status;
import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "seller_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(nullable = false)
    private String storeName;

    @Column(nullable = false)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String about;

    @Builder.Default
    @Column(nullable = false)
    private Double insideValleyDeliveryFee = 150.0;

    @Builder.Default
    @Column(nullable = false)
    private Double outsideValleyDeliveryFee = 200.0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean freeShippingEnabled = false;

    @Builder.Default
    @Column(nullable = false)
    private Double freeShippingMinOrder = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isVerified = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status = Status.PENDING;

    private LocalDateTime approvedAt;

    @Column(name = "joined_date")
    private LocalDateTime joinedDate;

    @PrePersist
    protected void onCreate() {
        if (joinedDate == null) {
            joinedDate = LocalDateTime.now();
        }
    }

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "logo_image_path")
    private String logoImagePath;

    @Builder.Default
    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal totalShippingCost = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal netIncome = BigDecimal.ZERO;

}