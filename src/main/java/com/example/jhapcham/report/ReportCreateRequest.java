package com.example.jhapcham.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCreateRequest {
    private ReportType type;
    private Long reportedEntityId;
    private String reason;
}
