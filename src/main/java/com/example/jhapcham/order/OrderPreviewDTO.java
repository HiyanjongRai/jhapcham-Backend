package com.example.jhapcham.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderPreviewDTO {

    private List<OrderItemResponseDTO> items;

    private BigDecimal itemsTotal;
    private BigDecimal shippingFee;
    private BigDecimal vatAmount;
    private BigDecimal discountTotal;
    private BigDecimal loyaltyDiscountAmount;
    private Long loyaltyPointsRedeemed;
    private BigDecimal grandTotal;

    private String estimatedDelivery; // "2-3 days" etc

}
