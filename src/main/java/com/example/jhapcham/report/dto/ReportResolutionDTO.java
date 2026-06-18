package com.example.jhapcham.report.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResolutionDTO {
    private String status;      // e.g. "ACTION_TAKEN", "CLOSED"
    private String action;      // e.g. "TAKEDOWN", "PENALIZE", "FLAG", "NO_ACTION"
    private String penaltyType; // for sellers: "WARNING", "SUSPENSION", "BAN"
    private String flagType;    // for customers: "WARNING", "BLOCKED"
    private String note;
}
