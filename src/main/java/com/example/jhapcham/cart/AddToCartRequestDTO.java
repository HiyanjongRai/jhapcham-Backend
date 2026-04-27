package com.example.jhapcham.cart;

import lombok.Data;

@Data
public class AddToCartRequestDTO {
    private Integer quantity;
    /**
     * The specific variant ID selected by the customer.
     * Replaces selectedColor, selectedStorage, selectedSize.
     */
    private Long variantId;
}
