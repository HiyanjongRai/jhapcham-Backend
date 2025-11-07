package com.example.jhapcham.product.model.ProductView;

import lombok.Getter;

@Getter
public class ProductViewCountDTO {
    private Long productId;
    private String productName;
    private Long views;

    public ProductViewCountDTO(Long productId, String productName, Long views) {
        this.productId = productId;
        this.productName = productName;
        this.views = views;
    }

}
