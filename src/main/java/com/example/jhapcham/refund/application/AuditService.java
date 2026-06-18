package com.example.jhapcham.refund.application;

import com.example.jhapcham.refund.domain.Refund;
import com.example.jhapcham.refund.domain.RefundAuditLog;
import com.example.jhapcham.refund.domain.RefundStatus;
import com.example.jhapcham.refund.persistence.RefundAuditLogRepository;
import com.example.jhapcham.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final RefundAuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void logTransition(Refund refund, RefundStatus oldStatus, RefundStatus newStatus, User actor, String notes) {
        RefundAuditLog auditLog = RefundAuditLog.builder()
                .refund(refund)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .actorId(actor != null ? actor.getId() : null)
                .actorRole(actor != null ? actor.getRole().name() : "SYSTEM")
                .notes(notes)
                .build();
        auditLogRepository.save(auditLog);
    }
}
