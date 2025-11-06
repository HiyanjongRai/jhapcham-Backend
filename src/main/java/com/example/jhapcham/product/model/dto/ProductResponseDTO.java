package com.example.jhapcham.product.model.dto;

import lombok.*;

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

    // NEW
    private Double averageRating; // e.g., 4.3
    private long ratingCount;     // e.g., 27

    private Double rating;        // legacy field on Product (kept if you still store it)
    private String status;        // ACTIVE, INACTIVE, DELETED, DRAFT
    private boolean visible;
}
