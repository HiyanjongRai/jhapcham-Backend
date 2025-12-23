package com.example.jhapcham.cart;

import lombok.Data;

@Data
public class AddToCartRequestDTO {
    private Integer quantity;
    private String selectedColor;
    private String selectedStorage;

}
