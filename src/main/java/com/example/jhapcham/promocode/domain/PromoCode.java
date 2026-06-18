package com.example.jhapcham.promocode.domain;


import com.example.jhapcham.promocode.application.*;
import com.example.jhapcham.promocode.domain.*;
import com.example.jhapcham.promocode.dto.*;
import com.example.jhapcham.promocode.persistence.*;
import com.example.jhapcham.campaign.domain.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 1000)
    private String bannerImage;

    @Column(nullable = true)
    private Long sellerId; // Null if global, but requirement says seller-specific initially

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private BigDecimal minOrderValue;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Integer usageLimit;

    @Column(nullable = false, columnDefinition = "integer default 1")
    @Builder.Default
    private Integer perUserUsageLimit = 1;

    @Column(nullable = false)
    private Integer usedCount;

    @Column(nullable = false)
    private Boolean isActive;

    // Optional: To strictly enforce scope
    @Enumerated(EnumType.STRING)
    private PromoScope scope;

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return Boolean.TRUE.equals(isActive) &&
                (usedCount < usageLimit) &&
                (now.isAfter(startDate) && now.isBefore(endDate));
    }

    public enum PromoScope {
        GLOBAL,
        SELLER_ONLY
    }
}
