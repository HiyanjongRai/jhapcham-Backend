package com.example.jhapcham.order;

import lombok.Data;

@Data
public class CheckoutItemDTO {

    private Long productId;
    private Integer quantity;
    private String selectedColor;
    private String selectedStorage;
}