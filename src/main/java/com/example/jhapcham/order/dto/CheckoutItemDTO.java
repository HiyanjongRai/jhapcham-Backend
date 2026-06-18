package com.example.jhapcham.order.dto;


import com.example.jhapcham.order.application.*;
import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.order.dto.*;
import com.example.jhapcham.order.persistence.*;
import lombok.Data;

@Data
public class CheckoutItemDTO {
    private Long productId;
    private Long variantId;   // Required: specific variant selected by customer
    private Integer quantity;
}