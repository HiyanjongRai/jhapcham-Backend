package com.example.jhapcham.refund.persistence;

import com.example.jhapcham.refund.domain.RefundEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundEvidenceRepository extends JpaRepository<RefundEvidence, Long> {
    List<RefundEvidence> findByRefundIdOrderByUploadedAtAsc(Long refundId);
}
