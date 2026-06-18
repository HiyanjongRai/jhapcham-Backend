package com.example.jhapcham.order.domain;


import com.example.jhapcham.order.application.*;
import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.order.dto.*;
import com.example.jhapcham.order.persistence.*;
import java.math.BigDecimal;

public record OrderPlacedEvent(
        Long orderId,
        String customOrderId,
        Long userId,
        String customerEmail,
        String customerName,
        BigDecimal grandTotal,
        String sellerEmail,
        String sellerName
) {
}

