package com.example.jhapcham.campaign.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CampaignType {
    FLASH_SALE("FLASH_SALE"),
    FESTIVAL("FESTIVAL"),
    SEASONAL("SEASONAL"),
    CLEARANCE("CLEARANCE"),
    OTHER("OTHER");

    private final String value;

    CampaignType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CampaignType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (CampaignType type : CampaignType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        try {
            return CampaignType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid CampaignType: " + value);
        }
    }
}
