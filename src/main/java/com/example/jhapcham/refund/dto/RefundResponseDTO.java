package com.example.jhapcham.refund.dto;

import com.example.jhapcham.refund.domain.InspectionVerdict;
import com.example.jhapcham.refund.domain.RefundStatus;
import com.example.jhapcham.refund.domain.RefundType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RefundResponseDTO {
    private Long id;
    private String refundNumber;
    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long sellerId;
    private String sellerName;
    private RefundType type;
    private RefundStatus status;
    private String reason;
    private String description;
    private InspectionVerdict verdict;
    private BigDecimal refundAmount;
    private Integer damageScore;
    private String inspectionNotes;
    private String trackingNumber;
    private String adminDecision;
    private Boolean returnRequired;
    private String paymentProofUrl;
    private String paymentReference;
    private String paymentComment;
    private String customerQrUrl;
    private String customerAccountDetails;
    private String replacementCourier;
    private String replacementTrackingNumber;
    private LocalDateTime replacementShippedAt;
    private String productImage;
    private List<RefundItemResponseDTO> items;
    private List<EvidenceResponseDTO> evidence;
    private InspectionResponseDTO inspection;
    private List<AuditLogResponseDTO> auditLogs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
