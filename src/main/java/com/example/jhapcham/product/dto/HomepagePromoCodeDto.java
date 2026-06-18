package com.example.jhapcham.product.dto;

import com.example.jhapcham.campaign.domain.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HomepagePromoCodeDto {
    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minPurchaseAmount;
    private LocalDateTime expiryDate;
    private String bannerImage;
    private Boolean activeStatus;
}
