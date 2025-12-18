package com.example.jhapcham.Cart;

import lombok.Data;

@Data
public class AddToCartRequestDTO {
    private Integer quantity;
    private String selectedColor;
    private String selectedStorage;


}
