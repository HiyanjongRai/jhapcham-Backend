package com.example.jhapcham.Cart;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CartResponseDTO {
    private Double subtotal;
    private List<CartItemResponseDTO> items;
}