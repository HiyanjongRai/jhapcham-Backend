package com.example.jhapcham.seller.persistence;

public interface TopSellerProjection {
    Long getSellerUserId();
    String getSellerFullName();
    String getStoreName();
    String getLogoImagePath();
    Long getSoldQuantity();
    Double getAverageRating();
    Long getTotalReviews();
}
