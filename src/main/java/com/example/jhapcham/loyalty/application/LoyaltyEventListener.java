package com.example.jhapcham.loyalty.application;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoyaltyEventListener {
    private final LoyaltyService loyaltyService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLoyaltyEvent(LoyaltyDomainEvent event) {
        try {
            switch (event.type()) {
                case CUSTOMER_REGISTERED -> {
                }
                case PAYMENT_COMPLETED -> loyaltyService.recordPaymentCompleted(event.orderId());
                case ORDER_DELIVERED -> loyaltyService.earnForDeliveredOrder(event.orderId(), event.customerId());
                case ORDER_REFUNDED -> {
                    if (event.amount() != null && event.sourceId() != null) {
                        loyaltyService.reverseForRefund(event.orderId(), event.customerId(), event.amount(), event.sourceId());
                    } else {
                        loyaltyService.reverseForRefundOrCancel(event.orderId(), event.customerId(), true);
                    }
                }
                case ORDER_CANCELLED -> loyaltyService.reverseForRefundOrCancel(event.orderId(), event.customerId(), false);
                case RETURN_WINDOW_CLOSED -> {
                }
            }
        } catch (Exception ex) {
            log.error("Failed to process loyalty event {}", event, ex);
        }
    }
}
