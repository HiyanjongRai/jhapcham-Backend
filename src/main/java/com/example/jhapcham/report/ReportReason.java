package com.example.jhapcham.report;

import lombok.Getter;

@Getter
public enum ReportReason {
    DAMAGED("Item is damaged"),
    WRONG_ITEM("Received the wrong item"),
    MISSING_ITEM("Item is missing from the package"),
    QUALITY_ISSUE("Quality does not meet expectations");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }
}
