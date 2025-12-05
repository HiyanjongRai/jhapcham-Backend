package com.example.jhapcham.activity;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ActivityItem {

    private ActivityType type;
    private Instant occurredAt;

    private Long productId;
    private String productName;

    // CATEGORY AND BRAND SUPPORT
    private String category;
    private String brand;

    // FOR CART AND ORDER
    private Integer quantity;
    private Double amount;
    private String selectedColor;
    private String selectedStorage;

    // REVIEW SUPPORT
    private Integer stars;
    private String text;
    private List<String> images;
    private boolean verifiedPurchase;


}