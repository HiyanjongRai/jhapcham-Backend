package com.example.jhapcham.order.domain;


import com.example.jhapcham.order.application.*;
import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.order.dto.*;
import com.example.jhapcham.order.persistence.*;
public enum OrderStatus {
    DRAFT,
    PENDING,
    COD_PENDING,
    CONFIRMED,
    CONFIRMED_BY_CALL,
    PROCESSING,
    PACKED,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED,
    REFUNDED,
    FAILED
}
