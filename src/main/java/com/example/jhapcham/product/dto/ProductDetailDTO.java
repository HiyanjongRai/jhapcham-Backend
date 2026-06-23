package com.example.jhapcham.product.dto;


import com.example.jhapcham.product.domain.*;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProductDetailDTO {

    private Long productId;
    private Long sellerProfileId;
    private Long sellerUserId;

    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private String category;
    private String brand;

    private String specification;
    private String storageSpec;
    private String features;
    private String colorOptions;

    private BigDecimal price;
    private BigDecimal discountPrice; // final discounted price (manual or calculated)
    private BigDecimal salePercentage; // NEW percentage discount field
    private BigDecimal salePrice;
    private String saleLabel;
    String saleType;
    // NEW calculated price after percentage

    private Boolean freeShipping;
    private Double insideValleyShipping;
    private Double outsideValleyShipping;
    private Double sellerFreeShippingMinOrder;

    private boolean onSale;
    private Integer stockQuantity;

    private Integer warrantyMonths;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;

    private ProductStatus status;
    private List<String> imagePaths;

    // seller info
    private String sellerUsername;
    private String sellerFullName;

    private String storeName;
    private String storeAddress;
    private String logoImagePath;
    private String profileImagePath;

    private Double averageRating;
    private Integer totalReviews;
    private java.time.LocalDateTime saleStartTime;
    private java.time.LocalDateTime saleEndTime;

    /** All active variants with their dynamic attributes — replaces colorOptions/storageSpec approach */
    private List<ProductVariantDTO> variants;
    /** All unique attribute groups across all variants — used to build UI selectors */
    private Map<String, List<AttributeOptionDTO>> attributeOptions;

    @com.fasterxml.jackson.annotation.JsonProperty("hasVariants")
    private boolean hasVariants;

    @Data
    @Builder
    public static class AttributeOptionDTO {
        private Long id;
        private String value;
    }
}
