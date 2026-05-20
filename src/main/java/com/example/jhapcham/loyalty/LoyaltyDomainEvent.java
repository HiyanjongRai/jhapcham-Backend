package com.example.jhapcham.loyalty;

import java.math.BigDecimal;

public record LoyaltyDomainEvent(LoyaltyEventType type, Long orderId, Long customerId, String referenceKey,
                                 BigDecimal amount, Long sourceId) {
    public LoyaltyDomainEvent(LoyaltyEventType type, Long orderId, Long customerId, String referenceKey) {
        this(type, orderId, customerId, referenceKey, null, null);
    }
}
