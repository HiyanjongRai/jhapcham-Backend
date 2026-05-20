package com.example.jhapcham.report;

import lombok.Getter;

@Getter
public enum ProductListingReportReason {
    DUPLICATE("Listing is a duplicate or replica"),
    INAPPROPRIATE("Content is inappropriate or offensive"),
    COUNTERFEIT("Item is fake or counterfeit"),
    SPAM("Listing is spam or misleading"),
    OTHER("Other policy violation");

    private final String description;

    ProductListingReportReason(String description) {
        this.description = description;
    }
}
