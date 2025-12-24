package com.example.jhapcham.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    private Long id;
    private ReportType type;
    private Long reportedEntityId;
    private String reportedEntityName;
    private String reportedEntityImage;
    private String reason;
    private Long reporterId;
    private String reporterName;
    private Long sellerUserId; // User ID of the seller to message
    private ReportStatus status;
    private LocalDateTime createdAt;
}
