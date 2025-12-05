package com.example.jhapcham.product.model.ProductView;

import java.time.LocalDateTime;

public class ProductViewDTO {

    private Long productId;
    private String productName;
    private String category;
    private String brand;          // ✅ ADD THIS
    private LocalDateTime viewedAt;

    // JPQL constructor
    public ProductViewDTO(
            Long productId,
            String productName,
            String category,
            LocalDateTime viewedAt
    ) {
        this.brand = brand;
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.viewedAt = viewedAt;
    }

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getCategory() { return category; }
    public String getBrand() { return brand; }         // ✅ ADD THIS
    public LocalDateTime getViewedAt() { return viewedAt; }
}
