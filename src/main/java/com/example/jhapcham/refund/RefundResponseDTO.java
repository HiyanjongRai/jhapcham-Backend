package com.example.jhapcham.refund;

import com.example.jhapcham.order.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RefundResponseDTO {
    private Long id;
    private Long orderId;
    private Long customerId;
    private String customerName;
    private Long sellerId;
    private String sellerName;
    private RefundReason reason;
    private String reasonDetails;
    private RefundStatus status;
    private PaymentMethod paymentMethod;
    private BigDecimal itemSubtotal;
    private BigDecimal taxRefund;
    private BigDecimal shippingRefund;
    private BigDecimal discountAdjustment;
    private BigDecimal totalRefund;
    private BigDecimal sellerCommissionReversal;
    private boolean shippingIncluded;
    private int fraudScore;
    private boolean fraudFlagged;
    private String customerNotes;
    private String sellerNotes;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime refundedAt;
    private LocalDateTime cancelledAt;
    private List<RefundLineItemResponseDTO> items;
    private List<RefundEvidenceDTO> evidence;
    private List<RefundTimelineDTO> timeline;
    private List<RefundTransactionDTO> transactions;
}
