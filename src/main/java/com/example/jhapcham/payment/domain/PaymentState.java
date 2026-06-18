package com.example.jhapcham.payment.domain;


import com.example.jhapcham.payment.application.*;
import com.example.jhapcham.payment.domain.*;
import com.example.jhapcham.payment.dto.*;
import com.example.jhapcham.payment.persistence.*;
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
