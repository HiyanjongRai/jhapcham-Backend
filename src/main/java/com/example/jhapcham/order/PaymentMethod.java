package com.example.jhapcham.order;

public enum PaymentMethod {
    COD,
    STRIPE,

    ESEWA,
    // Deprecated methods retained for database backward compatibility
    @Deprecated
    KHALTI,
    @Deprecated
    SKYPAY
}
