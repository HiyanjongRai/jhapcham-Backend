package com.example.jhapcham.order;

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

