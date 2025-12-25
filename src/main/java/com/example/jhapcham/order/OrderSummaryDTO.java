package com.example.jhapcham.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderSummaryDTO {

    private Long orderId;
    private OrderStatus status;

    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String shippingAddress;
    private String shippingLocation;
    private String customerProfileImagePath;
    private String customerAlternativePhone; // NEW
    private String deliveryTimePreference; // NEW
    private String orderNote; // NEW

    private PaymentMethod paymentMethod;
    private String paymentReference;

    private BigDecimal itemsTotal;
    private BigDecimal shippingFee;
    private BigDecimal discountTotal;
    private BigDecimal grandTotal;

    private LocalDateTime createdAt;

    private List<OrderItemResponseDTO> items;

    BigDecimal sellerGrossAmount;
    BigDecimal sellerShippingCharge;
    BigDecimal sellerNetAmount;
    DeliveryBranch deliveredBranch;

}