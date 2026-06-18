package com.example.jhapcham.refund.api;

import com.example.jhapcham.refund.application.RefundService;
import com.example.jhapcham.refund.dto.RefundResponseDTO;
import com.example.jhapcham.security.CurrentUserService;
import com.example.jhapcham.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRefundController {

    private final RefundService refundService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<List<RefundResponseDTO>> getAdminRefunds(Authentication authentication) {
        currentUserService.requireAdmin(currentUserService.requireUser(authentication));
        return ResponseEntity.ok(refundService.getAdminRefunds());
    }

    @GetMapping("/disputes")
    public ResponseEntity<List<RefundResponseDTO>> getDisputes(Authentication authentication) {
        currentUserService.requireAdmin(currentUserService.requireUser(authentication));
        return ResponseEntity.ok(refundService.getDisputes());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<RefundResponseDTO> adminApproveRefund(@PathVariable Long id,
                                                                Authentication authentication) {
        User admin = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(admin);
        return ResponseEntity.ok(refundService.adminApproveRefund(id, admin));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<RefundResponseDTO> adminRejectRefund(@PathVariable Long id,
                                                               Authentication authentication) {
        User admin = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(admin);
        return ResponseEntity.ok(refundService.adminRejectRefund(id, admin));
    }

    @PostMapping("/{id}/request-evidence")
    public ResponseEntity<RefundResponseDTO> requestEvidence(@PathVariable Long id,
                                                             Authentication authentication) {
        User admin = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(admin);
        return ResponseEntity.ok(refundService.adminRequestEvidence(id, admin));
    }

    @PostMapping("/{id}/process-payment")
    public ResponseEntity<RefundResponseDTO> processPayment(@PathVariable Long id,
                                                            Authentication authentication) {
        User admin = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(admin);
        return ResponseEntity.ok(refundService.processPayment(id, admin));
    }

    @PostMapping("/{id}/reject-payout")
    public ResponseEntity<RefundResponseDTO> rejectPayoutProof(@PathVariable Long id,
                                                               @RequestParam("reason") String reason,
                                                               Authentication authentication) {
        User admin = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(admin);
        return ResponseEntity.ok(refundService.rejectPayoutProof(id, reason, admin));
    }
}
