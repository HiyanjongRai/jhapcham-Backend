package com.example.jhapcham.promocode.dto;


import com.example.jhapcham.promocode.application.*;
import com.example.jhapcham.promocode.domain.*;
import com.example.jhapcham.promocode.dto.*;
import com.example.jhapcham.promocode.persistence.*;
import com.example.jhapcham.campaign.domain.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoCodeDTO {
    private Long id;
    private String code;
    private String description;
    private String bannerImage;
    private Long sellerId;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Integer perUserUsageLimit;
    private Integer usedCount;
    private Boolean isActive;
    // Helper for validation response
    private BigDecimal calculatedDiscount;
}
