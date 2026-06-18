package com.example.jhapcham.report.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerReportRequestDTO {
    private Long sellerId;
    private String reasonCode; // maps to SellerReportReason
    private String details;
}
