package com.example.jhapcham.cart.dto;


import com.example.jhapcham.cart.application.*;
import com.example.jhapcham.cart.domain.*;
import com.example.jhapcham.cart.dto.*;
import com.example.jhapcham.cart.persistence.*;
import lombok.Data;

@Data
public class AddToCartRequestDTO {
    private Integer quantity;
    /**
     * The specific variant ID selected by the customer.
     * Replaces selectedColor, selectedStorage, selectedSize.
     */
    private Long variantId;
}
