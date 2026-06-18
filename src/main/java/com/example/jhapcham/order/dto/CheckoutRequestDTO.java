package com.example.jhapcham.order.dto;


import com.example.jhapcham.order.application.*;
import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.order.dto.*;
import com.example.jhapcham.order.persistence.*;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequestDTO {

    private Long userId; // null for guest

    private String fullName;
    private String email;
    private String phone;
    private String alternativePhone; // NEW
    private String address;
    private String deliveryTimePreference; // NEW
    private String orderNote; // NEW

    // "INSIDE" or "OUTSIDE"
    private String shippingLocation;

    // "COD", "ESEWA", "STRIPE"
    private String paymentMethod;

    private List<CheckoutItemDTO> items;

    private String couponCode; // NEW

    private Long loyaltyPointsToRedeem;

    private String idempotencyKey;

}
