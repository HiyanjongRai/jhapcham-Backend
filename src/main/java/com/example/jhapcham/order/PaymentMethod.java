package com.example.jhapcham.order;

public enum PaymentMethod {
    COD,

    ESEWA,
    STRIPE,

    // Deprecated methods retained for database backward compatibility
    @Deprecated
    KHALTI,
    @Deprecated
    SKYPAY
}