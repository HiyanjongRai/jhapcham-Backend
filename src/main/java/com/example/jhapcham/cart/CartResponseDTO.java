package com.example.jhapcham.cart;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CartResponseDTO {
    private Double subtotal;
    private List<CartItemResponseDTO> items;
}