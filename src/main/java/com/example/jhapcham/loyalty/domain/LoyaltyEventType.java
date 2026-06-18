package com.example.jhapcham.loyalty.domain;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
public enum LoyaltyEventType {
    CUSTOMER_REGISTERED,
    PAYMENT_COMPLETED,
    ORDER_DELIVERED,
    ORDER_REFUNDED,
    ORDER_CANCELLED,
    RETURN_WINDOW_CLOSED
}
