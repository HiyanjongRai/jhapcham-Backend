package com.example.jhapcham.order;

import lombok.Data;

@Data
public class CartCheckoutRequestDTO {

    private Long userId;

    private String fullName;
    private String phone;
    private String email;
    private String address;
    private String alternativePhone; // NEW
    private String deliveryTimePreference; // NEW
    private String orderNote; // NEW

    // INSIDE or OUTSIDE
    private String shippingLocation;

    // COD, KHALTI, ESEWA, STRIPE
    private String paymentMethod;

}