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
public class MostWishlistedDTO {
    private Long id;
    private String name;
    private String slug;
    private String category;
    private String brand;
    private BigDecimal price;
    private BigDecimal salePrice;
    private boolean onSale;
    private Integer stockQuantity;
    private Boolean hasVariants;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private List<String> imagePaths;
    private Long wishlistCount;
    private Double averageRating;
    private Integer totalReviews;
    private String sellerFullName;
    private String storeName;
    private String logoImagePath;
}
