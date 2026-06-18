package com.example.jhapcham.admin.dto;


import com.example.jhapcham.admin.application.*;
import com.example.jhapcham.admin.dto.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionReportDTO {
    private Long orderId;
    private String customOrderId;
    /** Payment gateway transaction UUID (eSewa / Khalti). Null for COD orders. */
    private String transactionId;
    private String productName;
    private String category;
    private String sellerStoreName;
    private String sellerEmail;
    private String sellerPhone;
    private BigDecimal saleAmount;
    private Double commissionRate;
    private BigDecimal commissionEarned;
    private LocalDateTime createdAt;
    private com.example.jhapcham.order.domain.CommissionStatus status;
    private LocalDateTime dueDate;
    private BigDecimal fineAmount;
    private boolean isOverdue;
    private boolean reminderSent;
    private BigDecimal vatAmount;
    private BigDecimal discountTotal;
    private BigDecimal netAmount;
    private BigDecimal sellerPromoDiscountAmount;
    private BigDecimal platformSponsoredDiscountAmount;
    private BigDecimal inputVatAmount;
    private BigDecimal outputVatAmount;
    private BigDecimal vatPayableAmount;
    private BigDecimal grossProfitAmount;
    private BigDecimal netProfitAmount;
    private BigDecimal finalSellerEarnings;
}
