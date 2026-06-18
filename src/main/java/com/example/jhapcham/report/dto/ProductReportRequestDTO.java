package com.example.jhapcham.report.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReportRequestDTO {
    private Long productId;
    private String reasonCode; // maps to ProductReportReason
    private String details;
    private List<String> attachments;
}
