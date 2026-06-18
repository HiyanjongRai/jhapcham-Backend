package com.example.jhapcham.order.dto;


import com.example.jhapcham.order.application.*;
import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.order.dto.*;
import com.example.jhapcham.order.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderSummaryDTO {

    private Long orderId;
    private String customOrderId;  // e.g. JHC-20260520-0001
    private Long customerId; // NEW
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
    private PaymentStatus paymentStatus;
    private String paymentReference;
    private String trackingId;
    private com.example.jhapcham.delivery.domain.DeliveryStatus deliveryStatus;

    private BigDecimal itemsTotal;
    private BigDecimal shippingFee;
    private BigDecimal vatAmount;
    private BigDecimal discountTotal;
    private BigDecimal grandTotal;

    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
    private String appliedCoupon;

    private List<OrderItemResponseDTO> items;

    BigDecimal sellerGrossAmount;
    BigDecimal sellerShippingCharge;
    BigDecimal sellerNetAmount;
    BigDecimal marketplaceCommission;
    BigDecimal sellerPromoDiscountAmount;
    BigDecimal platformSponsoredDiscountAmount;
    BigDecimal inputVatAmount;
    BigDecimal outputVatAmount;
    BigDecimal vatPayableAmount;
    BigDecimal grossProfitAmount;
    BigDecimal netProfitAmount;
    BigDecimal finalSellerEarnings;
    DeliveryBranch deliveredBranch;


    // Seller Info
    private String sellerStoreName;
    private String sellerFullName;
    private Long sellerId;
    private String sellerEmail;
    private String sellerLogoPath;
}
