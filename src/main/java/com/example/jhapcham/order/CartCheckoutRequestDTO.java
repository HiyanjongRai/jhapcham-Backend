package com.example.jhapcham.order;

import lombok.Data;

@Data
public class CartCheckoutRequestDTO {

    private Long userId;

    private String fullName;
    private String phone;
    private String email;
    private String address;

    // INSIDE or OUTSIDE
    private String shippingLocation;

    // COD, KHALTI, ESEWA, STRIPE
    private String paymentMethod;


}