package com.example.jhapcham.order.domain;


import com.example.jhapcham.order.application.*;
import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.order.dto.*;
import com.example.jhapcham.order.persistence.*;
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
    PARTIALLY_REFUNDED,
    REFUNDED,
    FAILED,
    CANCELLED,
    EXPIRED
}
