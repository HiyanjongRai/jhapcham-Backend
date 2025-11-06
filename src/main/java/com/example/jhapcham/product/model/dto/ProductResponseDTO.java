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
    private String shortDescription;   // NEW
    private Double price;
    private String category;
    private Long sellerId;
    private String imagePath;
    private String others;
    private int stock;
    private int totalLikes;
    private int totalViews;
    private Double rating;
    private String status;
    private boolean visible;
}
