package com.example.jhapcham.order;

import com.example.jhapcham.payment.Payment;
import com.example.jhapcham.payment.PaymentRepository;
import com.example.jhapcham.payment.PaymentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCleanupTask {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStockService orderStockService;

    /**
     * Periodically cleans up orphaned or abandoned payment attempts.
     * Runs every 5 minutes.
     * Orders that stay in PAYMENT_INITIATED for more than 20 minutes are considered expired.
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    @Transactional
    public void cleanupExpiredPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(20);
        log.info("Running order cleanup task for abandoned payments before {}", cutoff);

        // Find orders that are still DRAFT and have not been successfully paid within the cutoff time
        List<Order> staleOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.DRAFT, cutoff);

        if (staleOrders.isEmpty()) {
            return;
        }

        log.info("Found {} stale orders to expire", staleOrders.size());

        for (Order order : staleOrders) {
            try {
                log.info("Expiring order #{}", order.getId());
                
                // Update Order Status
                order.setPaymentStatus(PaymentStatus.EXPIRED);
                order.setStatus(OrderStatus.CANCELLED);
                orderStockService.restoreStock(order);
                orderRepository.save(order);

                // Update Payment Entity if exists
                paymentRepository.findByOrder(order).ifPresent(payment -> {
                    payment.setState(PaymentState.EXPIRED);
                    payment.setUpdatedAt(LocalDateTime.now());
                    payment.setFailureReason("Payment session expired (auto-cleanup)");
                    paymentRepository.save(payment);
                });

            } catch (Exception e) {
                log.error("Failed to expire order #{}", order.getId(), e);
            }
        }
    }
}
