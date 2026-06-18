package com.example.jhapcham.refund.dto;

import com.example.jhapcham.refund.domain.InspectionVerdict;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InspectionResponseDTO {
    private Long id;
    private boolean physicalDamage;
    private boolean waterDamage;
    private boolean missingParts;
    private boolean burnDamage;
    private boolean tampering;
    private boolean packagingIntact;
    private boolean productMatches;
    private int severityScore;
    private String inspectorNotes;
    private InspectionVerdict verdict;
    private LocalDateTime createdAt;
}
