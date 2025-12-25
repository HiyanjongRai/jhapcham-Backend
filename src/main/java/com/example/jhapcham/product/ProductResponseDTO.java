package com.example.jhapcham.product;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ProductResponseDTO {

    private Long id;
    private Long sellerProfileId;

    private String name;
    private String shortDescription;
    private String description;
    private String category;
    private String brand;

    private String specification;
    private String storageSpec;
    private String features;
    private String colorOptions;

    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal salePercentage;
    private BigDecimal salePrice;
    private String saleLabel;

    private boolean onSale;
    private Integer stockQuantity;
    private Integer warrantyMonths;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;

    String saleType;

    private Boolean freeShipping;
    private Double insideValleyShipping;
    private Double outsideValleyShipping;
    private Double sellerFreeShippingMinOrder;

    private ProductStatus status;
    private List<String> imagePaths;
    private Long totalViews;
    private Double averageRating;
    private Integer totalReviews;
    private String sellerFullName;
    private String logoImagePath;
    private String profileImagePath;
    private java.time.LocalDateTime saleEndTime;
}
