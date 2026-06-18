package com.example.jhapcham.refund.application;

import com.example.jhapcham.refund.domain.*;
import com.example.jhapcham.refund.dto.InspectionRequestDTO;
import com.example.jhapcham.refund.persistence.RefundInspectionRepository;
import com.example.jhapcham.refund.persistence.RefundRepository;
import com.example.jhapcham.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private final RefundInspectionRepository inspectionRepository;
    private final RefundRepository refundRepository;
    private final RefundWorkflowService workflowService;
    private final AuditService auditService;

    @Transactional
    public Refund performInspection(Refund refund, InspectionRequestDTO dto, User inspector) {
        // State validation
        workflowService.validateTransition(refund.getStatus(), RefundStatus.INSPECTION_COMPLETE);

        // Count how many checks are evaluated.
        // A check is considered completed if the inspector provided it.
        // On backend, we receive all 7 boolean flags. We require severity and verdict to be present.
        if (dto.getSeverityScore() == null || dto.getSeverityScore() < 1 || dto.getSeverityScore() > 10) {
            throw new IllegalArgumentException("Severity score (1-10) is required");
        }
        if (dto.getVerdict() == null) {
            throw new IllegalArgumentException("Verdict is required");
        }

        // Create or update inspection
        RefundInspection inspection = refund.getInspection();
        if (inspection == null) {
            inspection = new RefundInspection();
            inspection.setRefund(refund);
        }

        inspection.setPhysicalDamage(dto.isPhysicalDamage());
        inspection.setWaterDamage(dto.isWaterDamage());
        inspection.setMissingParts(dto.isMissingParts());
        inspection.setBurnDamage(dto.isBurnDamage());
        inspection.setTampering(dto.isTampering());
        inspection.setPackagingIntact(dto.isPackagingIntact());
        inspection.setProductMatches(dto.isProductMatches());
        inspection.setSeverityScore(dto.getSeverityScore());
        inspection.setInspectorNotes(dto.getInspectorNotes());
        inspection.setVerdict(dto.getVerdict());

        inspectionRepository.save(inspection);

        // Update refund record
        RefundStatus oldStatus = refund.getStatus();
        refund.setStatus(RefundStatus.INSPECTION_COMPLETE);
        refund.setVerdict(dto.getVerdict());
        refund.setDamageScore(dto.getSeverityScore());
        refund.setInspectionNotes(dto.getInspectorNotes());
        refund.setInspection(inspection);

        Refund saved = refundRepository.save(refund);

        // Log transition
        auditService.logTransition(refund, oldStatus, RefundStatus.INSPECTION_COMPLETE, inspector,
                "Inspection completed. Verdict: " + dto.getVerdict() + ", Severity: " + dto.getSeverityScore());

        return saved;
    }
}
