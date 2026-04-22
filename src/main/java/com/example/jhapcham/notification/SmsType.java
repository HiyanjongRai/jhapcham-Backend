package com.example.jhapcham.notification;

public enum SmsType {
    ORDER_CONFIRMATION("Order confirmation OTP"),
    SHIPMENT_NOTIFICATION("Shipment update"),
    DELIVERY_OTP("Delivery verification OTP"),
    REFUND_ALERT("Refund status alert"),
    DISPUTE_ALERT("Dispute notification"),
    INVENTORY_ALERT("Inventory warning");

    private final String description;

    SmsType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
