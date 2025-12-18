package com.example.jhapcham.Cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponseDTO {
    private Long cartItemId;
    private Long productId;
    private String name;
    private String brand;
    private String image;
    private Integer quantity;
    private BigDecimal price;
    private String selectedColor;
    private String selectedStorage;
}