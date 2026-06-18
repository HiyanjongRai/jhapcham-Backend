package com.example.jhapcham.cart.dto;


import com.example.jhapcham.cart.application.*;
import com.example.jhapcham.cart.domain.*;
import com.example.jhapcham.cart.dto.*;
import com.example.jhapcham.cart.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CartResponseDTO {
    private Double subtotal;
    private List<CartItemResponseDTO> items;
}