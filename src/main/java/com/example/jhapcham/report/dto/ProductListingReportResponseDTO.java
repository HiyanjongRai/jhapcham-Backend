package com.example.jhapcham.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductListingReportResponseDTO {
    private Long id;
    private String reportId;
    private Long productId;
    private String productName;
    private String productSlug;
    private String productImageUrl;
    private Long reporterId;
    private String reporterName;
    private String reason;
    private String reasonDescription;
    private String description;
    private String status;
    private String adminComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
