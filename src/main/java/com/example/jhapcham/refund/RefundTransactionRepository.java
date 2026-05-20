package com.example.jhapcham.refund;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {
    Optional<RefundTransaction> findByProviderRefundReference(String providerRefundReference);
    Optional<RefundTransaction> findByIdempotencyKey(String idempotencyKey);
}
