package com.example.jhapcham.banner;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BannerProductResponseDTO {
    private Long id;
    private Long productId;
    private String name;
    private String thumbnail;
    private Integer stock;
    private BigDecimal price;
    private BigDecimal discountPercentage;
    private BigDecimal discountedPrice;
    private Integer displayOrder;
}
