package com.example.jhapcham.cart;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartItemDto {
    private Long productId;
    private String name;
    private String imagePath;
    private Double unitPrice;
    private int quantity;
    private Double lineTotal; // unitPrice * quantity
}
