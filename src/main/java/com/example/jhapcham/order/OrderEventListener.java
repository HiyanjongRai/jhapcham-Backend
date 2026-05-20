package com.example.jhapcham.order;

import com.example.jhapcham.config.AsyncConfig;
import com.example.jhapcham.notification.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final EmailService emailService;

    @Async(AsyncConfig.DOMAIN_EVENT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        // Use the human-readable JHC-YYYYMMDD-XXXX reference in emails.
        // Fall back to the numeric DB id for legacy orders that pre-date the field.
        String orderRef = event.customOrderId() != null
                ? event.customOrderId()
                : String.valueOf(event.orderId());

        try {
            emailService.sendOrderConfirmationToCustomer(
                    event.customerEmail(),
                    event.customerName(),
                    orderRef,
                    event.grandTotal());

            if (event.sellerEmail() != null && !event.sellerEmail().isBlank()) {
                emailService.sendNewOrderAlertToSeller(
                        event.sellerEmail(),
                        event.sellerName(),
                        orderRef,
                        event.grandTotal());
            }
        } catch (Exception ex) {
            log.error("Failed to send async order placement notifications for order {} [{}]",
                    orderRef, event.orderId(), ex);
        }
    }
}

