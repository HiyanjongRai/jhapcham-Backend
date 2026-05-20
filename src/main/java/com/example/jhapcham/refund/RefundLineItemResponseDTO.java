package com.example.jhapcham.refund;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RefundLineItemResponseDTO {
    private Long id;
    private Long orderItemId;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantityRequested;
    private BigDecimal itemSubtotal;
    private BigDecimal taxRefund;
    private BigDecimal discountAdjustment;
    private BigDecimal totalRefund;
    private BigDecimal sellerCommissionReversal;
    private boolean restockInventory;
}
