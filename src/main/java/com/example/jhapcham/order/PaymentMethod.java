package com.example.jhapcham.order;

public enum PaymentMethod {
    COD,
    STRIPE,

    ESEWA,
    KHALTI,
    // Deprecated methods retained for database backward compatibility
    @Deprecated
    SKYPAY
}
