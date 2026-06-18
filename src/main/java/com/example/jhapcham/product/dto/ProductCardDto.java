package com.example.jhapcham.product.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductCardDto {
    private Long id;
    private String name;
    private String thumbnail;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal salePrice;
    private BigDecimal salePercentage;
    private String saleLabel;
    private Boolean onSale;
    private Double rating;
    private Integer totalReviews;
    private String stockStatus;
    private String category;
    private String brand;
    private Boolean hasVariants;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
