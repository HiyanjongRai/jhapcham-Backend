package com.example.jhapcham.order.domain;


import com.example.jhapcham.order.application.*;
import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.order.dto.*;
import com.example.jhapcham.order.persistence.*;
public enum PaymentMethod {
    COD,
    STRIPE,

    ESEWA,
    KHALTI,
    // Deprecated methods retained for database backward compatibility
    @Deprecated
    SKYPAY
}
