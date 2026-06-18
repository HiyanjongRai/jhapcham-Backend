package com.example.jhapcham.order.dto;


import com.example.jhapcham.order.application.*;
import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.order.dto.*;
import com.example.jhapcham.order.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderListItemDTO {

    private Long orderId;
    private String customOrderId;  // e.g. JHC-20260520-0001
    private Long customerId;
    private OrderStatus status;
    private BigDecimal grandTotal;
    private BigDecimal itemsTotal;
    private BigDecimal discountTotal;
    private Integer totalItems;
    private LocalDateTime createdAt;
    private String appliedCoupon;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String paymentReference;
    private com.example.jhapcham.delivery.domain.DeliveryStatus deliveryStatus;
    private String storeName; // for seller view
    private String customerName;
    private String customerPhone;
    private String orderNote;
    private String deliveryTimePreference;

    private BigDecimal sellerGrossAmount;
    private BigDecimal sellerShippingCharge;
    private BigDecimal vatAmount;
    private BigDecimal marketplaceCommission;
    private BigDecimal sellerNetAmount;
    private DeliveryBranch deliveredBranch;
    private DeliveryBranch assignedBranch;
    private String productNames;
    private String productImage;
    private String customerProfileImagePath;

}
