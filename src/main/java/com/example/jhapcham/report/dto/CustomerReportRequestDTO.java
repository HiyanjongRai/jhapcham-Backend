package com.example.jhapcham.report.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerReportRequestDTO {
    private Long customerId;
    private Long orderId;
    private String reasonCode; // maps to CustomerReportReason
    private String details;
}
