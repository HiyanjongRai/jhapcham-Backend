package com.example.jhapcham.order;

import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequestDTO {

    private Long userId;                 // null for guest

    private String fullName;
    private String email;
    private String phone;
    private String address;

    // "INSIDE" or "OUTSIDE"
    private String shippingLocation;

    // "COD", "KHALTI", "ESEWA", "STRIPE"
    private String paymentMethod;

    private List<CheckoutItemDTO> items;


}