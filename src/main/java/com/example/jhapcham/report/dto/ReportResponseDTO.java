package com.example.jhapcham.report.dto;

import com.example.jhapcham.report.ReportReason;
import com.example.jhapcham.report.ReportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReportResponseDTO {
    private Long id;
    private String reportId;
    private Long orderId;
    private Long orderItemId;
    private Long reportedEntityId;
    private String reportedEntityName;
    private String reportedEntityImage;
    private String type;
    private String productName;
    private String productImage;
    private Long customerId;
    private String customerName;
    private Long reporterId;
    private String reporterName;
    private Long sellerId;
    private String storeName;
    private ReportReason reason;
    private String description;
    private List<String> evidenceUrls;
    private ReportStatus status;
    private String sellerComment;
    private String adminComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Linked refund info if exists
    private Long refundId;
    private String refundStatus;
}
