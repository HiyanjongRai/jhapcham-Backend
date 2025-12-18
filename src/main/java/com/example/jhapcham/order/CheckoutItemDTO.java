package com.example.jhapcham.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckoutItemDTO {

    private Long productId;
    private Integer quantity;
    private String selectedColor;
    private String selectedStorage;
}