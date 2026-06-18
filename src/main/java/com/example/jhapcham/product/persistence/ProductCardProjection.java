package com.example.jhapcham.product.persistence;


import com.example.jhapcham.product.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ProductCardProjection {
    Long getId();
    Long getSellerProfileId();
    Long getSellerUserId();
    String getName();
    String getSlug();
    String getShortDescription();
    String getDescription();
    String getCategory();
    String getBrand();
    BigDecimal getPrice();
    BigDecimal getDiscountPrice();
    BigDecimal getSalePercentage();
    BigDecimal getSalePrice();
    String getSaleLabel();
    Boolean getOnSale();
    Integer getStockQuantity();
    Integer getWarrantyMonths();

    LocalDate getManufactureDate();
    LocalDate getExpiryDate();
    Boolean getFreeShipping();
    Double getInsideValleyShipping();
    Double getOutsideValleyShipping();
    Double getSellerFreeShippingMinOrder();
    String getStatus();
    String getImagePaths();
    Long getTotalViews();
    Double getAverageRating();
    Integer getTotalReviews();
    String getSellerFullName();
    String getStoreName();
    String getLogoImagePath();
    String getProfileImagePath();
    LocalDateTime getSaleEndTime();
    Boolean getHasVariants();
    Boolean getFeatured();
    LocalDateTime getCreatedAt();
    BigDecimal getMinPrice();
    BigDecimal getMaxPrice();
}
