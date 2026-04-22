package com.example.jhapcham.dispute;

public enum DisputeStatus {
    OPENED("Dispute opened"),
    UNDER_REVIEW("Under review by admin"),
    EVIDENCE_REQUIRED("Waiting for evidence from parties"),
    IN_DISCUSSION("Parties discussing resolution"),
    RESOLVED("Resolved"),
    CANCELLED("Cancelled");

    private final String description;

    DisputeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
