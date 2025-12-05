package com.example.jhapcham.cart;

import lombok.Data;

@Data
public class CartAddRequest {
    private Long userId;
    private Long productId;
    private int quantity;
    private String color;
    private String storage;
}
