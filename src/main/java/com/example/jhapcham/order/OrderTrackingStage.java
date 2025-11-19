package com.example.jhapcham.order;

public enum OrderTrackingStage {
    PROCESSING,
    SENT_TO_BRANCH,
    ARRIVED_AT_BRANCH,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}