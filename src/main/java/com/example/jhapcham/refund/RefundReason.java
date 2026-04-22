package com.example.jhapcham.refund;

public enum RefundReason {
    PRODUCT_DEFECTIVE("Product arrived defective"),
    NOT_AS_DESCRIBED("Product not as described"),
    WRONG_ITEM("Wrong item received"),
    DAMAGED_IN_SHIPPING("Damaged during shipping"),
    CHANGED_MIND("Changed mind about purchase"),
    FOUND_BETTER_PRICE("Found better price elsewhere"),
    DUPLICATE_ORDER("Duplicate order"),
    OTHER("Other reason");

    private final String description;

    RefundReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
