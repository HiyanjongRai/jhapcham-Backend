package com.example.jhapcham.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    List<PaymentEvent> findByPayment_IdOrderByCreatedAtDesc(Long paymentId);
}

