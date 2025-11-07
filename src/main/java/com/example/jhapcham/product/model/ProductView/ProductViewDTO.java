package com.example.jhapcham.product.model.ProductView;

import java.time.LocalDateTime;

/**
 * Lightweight DTO for returning product view history.
 */
public class ProductViewDTO {

    private Long productId;
    private String productName;
    private String category;
    private LocalDateTime viewedAt;

    public ProductViewDTO(Long productId, String productName, String category, LocalDateTime viewedAt) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.viewedAt = viewedAt;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }
}
