package com.example.jhapcham.common;

public enum PublicReferenceType {
    PRODUCT_REPORT("PRD-RPT"),
    SELLER_REPORT("SLR-RPT"),
    CUSTOMER_REPORT("CUS-RPT"),
    REFUND("REF");

    private final String prefix;

    PublicReferenceType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
