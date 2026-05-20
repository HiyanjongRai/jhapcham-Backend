package com.example.jhapcham.dispute;

public enum DisputeStatus {
    OPENED("Dispute opened"),
    WAITING_FOR_SELLER("Waiting for seller to respond"),
    WAITING_FOR_CUSTOMER("Waiting for customer to respond"),
    ESCALATED("Escalated to platform admin"),
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
