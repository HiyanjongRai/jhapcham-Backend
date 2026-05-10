package com.example.jhapcham.order;

public enum PaymentStatus {
    PENDING,
    REQUIRES_PAYMENT,
    PAYMENT_INITIATED,
    PAID,
    PENDING_VERIFICATION,
    PENDING_COD,
    COD_COLLECTED,
    COD_FAILED,
    COD_REMITTED,
    REFUND_PENDING,
    REFUNDED,
    FAILED,
    CANCELLED,
    EXPIRED
}
