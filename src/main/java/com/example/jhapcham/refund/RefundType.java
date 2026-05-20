package com.example.jhapcham.refund;

public enum RefundType {
    REFUND_ONLY("Refund only — keep the product"),
    RETURN_AND_REFUND("Return product and receive a full refund"),
    REPLACEMENT("Keep the product and receive a replacement unit");

    private final String description;

    RefundType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
