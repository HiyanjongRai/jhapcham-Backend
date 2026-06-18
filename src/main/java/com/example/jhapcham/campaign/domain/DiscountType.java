package com.example.jhapcham.campaign.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DiscountType {
    PERCENTAGE("PERCENTAGE"),
    FIXED_AMOUNT("FIXED_AMOUNT"),
    FLAT("FLAT");

    private final String value;

    DiscountType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DiscountType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (DiscountType type : DiscountType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        // Handle case-insensitive matching
        try {
            return DiscountType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid DiscountType: " + value);
        }
    }
}
