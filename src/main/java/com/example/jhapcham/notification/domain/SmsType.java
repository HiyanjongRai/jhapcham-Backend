package com.example.jhapcham.notification.domain;


import com.example.jhapcham.notification.application.*;
import com.example.jhapcham.notification.domain.*;
import com.example.jhapcham.notification.dto.*;
import com.example.jhapcham.notification.persistence.*;
public enum SmsType {
    ORDER_CONFIRMATION("Order confirmation OTP"),
    SHIPMENT_NOTIFICATION("Shipment update"),
    DELIVERY_OTP("Delivery verification OTP"),
    INVENTORY_ALERT("Inventory warning");

    private final String description;

    SmsType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
