package com.example.jhapcham.refund.persistence;

import com.example.jhapcham.refund.domain.RefundInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundInspectionRepository extends JpaRepository<RefundInspection, Long> {
    Optional<RefundInspection> findByRefundId(Long refundId);
}
