package com.example.jhapcham.seller.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SellerProfileStatsDto {

    private String storeName;
    private String address;
    private String contactNumber;

    private long totalProducts;
    private long totalLikes;
    private double averageRating;
}
