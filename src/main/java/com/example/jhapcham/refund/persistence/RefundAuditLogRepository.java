package com.example.jhapcham.refund.persistence;

import com.example.jhapcham.refund.domain.RefundAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundAuditLogRepository extends JpaRepository<RefundAuditLog, Long> {
    List<RefundAuditLog> findByRefundIdOrderByCreatedAtAsc(Long refundId);
}
