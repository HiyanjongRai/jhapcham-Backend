package com.example.jhapcham.order;

import lombok.Data;

@Data
public class CheckoutItemDTO {
    private Long productId;
    private Long variantId;   // Required: specific variant selected by customer
    private Integer quantity;
}