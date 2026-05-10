package com.example.jhapcham.refund;

import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.notification.EmailService;
import com.example.jhapcham.notification.NotificationService;
import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderRepository;
import com.example.jhapcham.order.OrderStatus;
import com.example.jhapcham.order.PaymentStatus;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Transactional
    public Refund completeRefund(Long refundId, String adminNotes) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));

        if (refund.getStatus() == RefundStatus.COMPLETED) {
            throw new RuntimeException("Refund is already completed");
        }

        refund.setStatus(RefundStatus.COMPLETED);
        refund.setAdminNotes(adminNotes);
        refund.setCompletedAt(LocalDateTime.now());
        
        Refund saved = refundRepository.save(refund);

        // Update Order Status
        Order order = refund.getOrder();
        updateOrderStatusAfterRefund(order);

        // Notify Customer
        notificationService.createNotification(refund.getCustomer(), "Refund Completed", 
            "Your refund of Rs. " + refund.getAmount() + " has been successfully processed.", 
            com.example.jhapcham.notification.NotificationType.ORDER_UPDATE, refund.getId());

        log.info("Refund {} completed by admin", refundId);
        return saved;
    }

    private void updateOrderStatusAfterRefund(Order order) {
        long totalItems = order.getItems().size();
        // Count unique order items that have a COMPLETED refund
        // This is a simplification; a real system might check the total refunded amount vs total order amount
        long refundedItems = order.getItems().stream()
                .filter(item -> refundRepository.findRefundByOrderItemAndStatus(item, RefundStatus.COMPLETED).isPresent())
                .count();

        if (refundedItems >= totalItems) {
            order.setStatus(OrderStatus.REFUNDED);
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        } else if (refundedItems > 0 && order.getPaymentStatus() != PaymentStatus.REFUNDED) {
            order.setRefundPending(true);
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        }
        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Refund> getSellerRefunds(Long sellerId) {
        User seller = userRepository.findById(sellerId).orElseThrow();
        return refundRepository.findBySellerOrderByCreatedAtDesc(seller);
    }

    @Transactional(readOnly = true)
    public List<Refund> getPendingPayouts() {
        return refundRepository.findByStatusOrderByCreatedAtDesc(RefundStatus.PENDING);
    }
}
