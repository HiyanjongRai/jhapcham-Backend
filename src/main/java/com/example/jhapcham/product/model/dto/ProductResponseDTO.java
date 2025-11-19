package com.example.jhapcham.product.model.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String shortDescription;
    private Double price;
    private String category;
    private Long sellerId;
    private String imagePath;
    private String others;
    private int stock;

    private int totalLikes;
    private int totalViews;

    private Double averageRating;
    private long ratingCount;

    private Double rating;
    private String status;
    private boolean visible;

    private List<String> colors;

    private boolean onSale;
    private Double discountPercent;
    private Double salePrice;

    private String brand;
    private String sellerStoreName;
    private String sellerStoreAddress;
    private String sellerContactNumber;

}