package com.example.jhapcham.refund.dto;

import com.example.jhapcham.refund.domain.InspectionVerdict;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InspectionRequestDTO {
    private boolean physicalDamage;
    private boolean waterDamage;
    private boolean missingParts;
    private boolean burnDamage;
    private boolean tampering;
    private boolean packagingIntact;
    private boolean productMatches;

    @NotNull(message = "Severity score is required")
    @Min(value = 1, message = "Severity score must be at least 1")
    @Max(value = 10, message = "Severity score must be at most 10")
    private Integer severityScore;

    private String inspectorNotes;

    @NotNull(message = "Verdict is required")
    private InspectionVerdict verdict;
}
