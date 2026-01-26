package com.example.jhapcham.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderListItemDTO {

    private Long orderId;
    private OrderStatus status;
    private BigDecimal grandTotal;
    private Integer totalItems;
    private LocalDateTime createdAt;
    private String storeName; // for seller view
    private String customerName;
    private String customerPhone;
    private String orderNote;
    private String deliveryTimePreference;

    private BigDecimal sellerGrossAmount;
    private BigDecimal sellerShippingCharge;
    private BigDecimal sellerNetAmount;
    private DeliveryBranch deliveredBranch;
    private DeliveryBranch assignedBranch;
    private String productNames;
    private String productImage;
    private String customerProfileImagePath;

}