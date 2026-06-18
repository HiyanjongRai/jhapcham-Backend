package com.example.jhapcham.payment.application;


import com.example.jhapcham.payment.application.*;
import com.example.jhapcham.payment.domain.*;
import com.example.jhapcham.payment.dto.*;
import com.example.jhapcham.payment.persistence.*;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.order.domain.Order;
import com.example.jhapcham.order.persistence.OrderRepository;
import com.example.jhapcham.order.domain.OrderStatus;
import com.example.jhapcham.order.domain.PaymentStatus;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentWorkflowService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final OrderRepository orderRepository;

    public void assertCustomerPaymentOwnership(List<Order> orders, User actor) {
        if (actor == null || actor.getRole() == Role.ADMIN) {
            return;
        }
        for (Order order : orders) {
            if (order.getUser() == null || !order.getUser().getId().equals(actor.getId())) {
                throw new BusinessValidationException("You do not have permission to pay order #" + order.getId());
            }
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEvent(Payment payment, PaymentEventType type, String payload) {
        paymentEventRepository.save(PaymentEvent.builder()
                .payment(payment)
                .type(type)
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markVerificationFailed(List<Order> orders, String reason, boolean preserveSuccessfulPayment) {
        for (Order order : orders) {
            if (order.getPaymentStatus() != PaymentStatus.PAID && order.getStatus() == OrderStatus.DRAFT) {
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);
            }

            paymentRepository.findByOrder(order).ifPresent(payment -> {
                if (!preserveSuccessfulPayment || payment.getState() != PaymentState.SUCCESS) {
                    payment.setState(PaymentState.FAILED);
                    payment.setFailureReason(reason);
                    payment.setUpdatedAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }
                recordEvent(payment, PaymentEventType.VERIFICATION_FAILED, reason);
            });
        }
    }
}
