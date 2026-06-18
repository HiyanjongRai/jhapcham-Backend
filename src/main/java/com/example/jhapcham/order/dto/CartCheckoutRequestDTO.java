package com.example.jhapcham.order.dto;


import com.example.jhapcham.order.application.*;
import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.order.dto.*;
import com.example.jhapcham.order.persistence.*;
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

    // COD, ESEWA, STRIPE
    private String paymentMethod;

    private String couponCode; // Add this

    private Long loyaltyPointsToRedeem;

    private String idempotencyKey;

}
