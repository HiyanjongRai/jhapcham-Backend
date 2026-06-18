package com.example.jhapcham.seller.dto;

import com.example.jhapcham.seller.persistence.TopSellerProjection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopSellerDTO {
    private Long sellerUserId;
    private String sellerFullName;
    private String storeName;
    private String logoImagePath;
    private Long soldQuantity;
    private Double averageRating;
    private Long totalReviews;

    public static TopSellerDTO from(TopSellerProjection projection) {
        return TopSellerDTO.builder()
                .sellerUserId(projection.getSellerUserId())
                .sellerFullName(projection.getSellerFullName())
                .storeName(projection.getStoreName())
                .logoImagePath(projection.getLogoImagePath())
                .soldQuantity(projection.getSoldQuantity())
                .averageRating(projection.getAverageRating())
                .totalReviews(projection.getTotalReviews())
                .build();
    }
}
