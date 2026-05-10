package com.example.jhapcham.payment;

public enum PaymentState {
    INITIATED,
    PENDING,
    PENDING_VERIFICATION,
    SUCCESS,
    FAILED,
    REFUNDED,
    CANCELLED,
    EXPIRED
}
