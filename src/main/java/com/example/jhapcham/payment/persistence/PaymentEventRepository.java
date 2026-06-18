package com.example.jhapcham.payment.persistence;


import com.example.jhapcham.payment.application.*;
import com.example.jhapcham.payment.domain.*;
import com.example.jhapcham.payment.dto.*;
import com.example.jhapcham.payment.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    List<PaymentEvent> findByPayment_IdOrderByCreatedAtDesc(Long paymentId);
}

