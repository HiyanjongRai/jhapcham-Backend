package com.example.jhapcham.delivery.domain;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
public enum DeliveryStatus {
    CREATED,
    RIDER_ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED_DELIVERY,
    RETURN_TO_SELLER,
    DELAYED,
    CANCELLED,
    CALL_NOT_PICKED,
    ADDRESS_NOT_FOUND
}
