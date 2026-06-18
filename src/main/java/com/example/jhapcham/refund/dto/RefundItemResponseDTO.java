package com.example.jhapcham.refund.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RefundItemResponseDTO {
    private Long id;
    private Long orderItemId;
    private String productName;
    private String imagePath;
    private Integer quantity;
    private BigDecimal refundAmount;
}
