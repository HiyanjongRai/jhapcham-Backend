package com.example.jhapcham.refund;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestDTO {
    private Long orderId;
    private Long orderItemId;
    private RefundReason reason;
    private String reasonDetails;
}
