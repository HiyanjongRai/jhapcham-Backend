package com.example.jhapcham.refund;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundEvidenceRepository extends JpaRepository<RefundEvidence, Long> {
    List<RefundEvidence> findByRefundRequestOrderByCreatedAtAsc(RefundRequest refundRequest);
}
