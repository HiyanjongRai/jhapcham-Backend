package com.example.jhapcham.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDTO {
    private Long id;
    private String name;
    private String slug;
    private String category;
    private String brand;
    private BigDecimal price;
    private BigDecimal salePrice;
    private boolean onSale;
    private Integer stockQuantity;
    private List<String> imagePaths;
    private Double averageRating;
    private Integer totalReviews;
    private Long totalViews;
    private Boolean hasVariants;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String recommendationReason;
    private String sellerFullName;
    private String storeName;
    private String logoImagePath;
}
