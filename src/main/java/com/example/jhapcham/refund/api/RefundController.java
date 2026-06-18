package com.example.jhapcham.refund.api;

import com.example.jhapcham.refund.application.RefundService;
import com.example.jhapcham.refund.dto.*;
import com.example.jhapcham.security.CurrentUserService;
import com.example.jhapcham.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<RefundResponseDTO> createRefund(@Valid @RequestBody RefundRequestDTO req,
                                                          Authentication authentication) {
        User customer = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.createRefund(req, customer));
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<RefundResponseDTO> uploadEvidence(@PathVariable Long id,
                                                            @Valid @RequestBody EvidenceRequestDTO req,
                                                            Authentication authentication) {
        User customer = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.uploadEvidence(id, req, customer));
    }

    @PostMapping("/{id}/tracking")
    public ResponseEntity<RefundResponseDTO> uploadTracking(@PathVariable Long id,
                                                            @Valid @RequestBody TrackingRequestDTO req,
                                                            Authentication authentication) {
        User customer = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.uploadTracking(id, req, customer));
    }

    @PostMapping("/{id}/appeal")
    public ResponseEntity<RefundResponseDTO> appealDecision(@PathVariable Long id,
                                                            @Valid @RequestBody AppealRequestDTO req,
                                                            Authentication authentication) {
        User customer = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.appealDecision(id, req, customer));
    }

    @PostMapping("/{id}/accept-offer")
    public ResponseEntity<RefundResponseDTO> acceptOffer(@PathVariable Long id,
                                                         Authentication authentication) {
        User customer = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.acceptOffer(id, customer));
    }

    @PostMapping("/{id}/accept-exchange")
    public ResponseEntity<RefundResponseDTO> acceptExchange(@PathVariable Long id,
                                                            Authentication authentication) {
        User customer = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.acceptExchange(id, customer));
    }

    @PostMapping("/{id}/reject-exchange")
    public ResponseEntity<RefundResponseDTO> rejectExchange(@PathVariable Long id,
                                                            @Valid @RequestBody RejectExchangeRequestDTO dto,
                                                            Authentication authentication) {
        User customer = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.rejectExchange(id, dto, customer));
    }

    @PostMapping("/{id}/payout-details")
    public ResponseEntity<RefundResponseDTO> submitPayoutDetails(@PathVariable Long id,
                                                                 @RequestBody CustomerPayoutDetailsRequestDTO req,
                                                                 Authentication authentication) {
        User customer = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.submitPayoutDetails(id, req, customer));
    }

    @PostMapping("/{id}/reject-offer")
    public ResponseEntity<RefundResponseDTO> rejectOffer(@PathVariable Long id,
                                                         Authentication authentication) {
        User customer = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.rejectOffer(id, customer));
    }

    @PostMapping("/{id}/negotiate")
    public ResponseEntity<RefundResponseDTO> negotiateOffer(@PathVariable Long id,
                                                            @RequestParam("notes") String notes,
                                                            @RequestParam(value = "amount", required = false) java.math.BigDecimal amount,
                                                            Authentication authentication) {
        User customer = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.negotiateOffer(id, notes, amount, customer));
    }

    @PostMapping("/{id}/escalate-offer")
    public ResponseEntity<RefundResponseDTO> escalateOffer(@PathVariable Long id,
                                                           Authentication authentication) {
        User customer = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.escalateOffer(id, customer));
    }

    @GetMapping("/my")
    public ResponseEntity<List<RefundResponseDTO>> getMyRefunds(Authentication authentication) {
        User customer = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.getMyRefunds(customer));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RefundResponseDTO> getRefundDetails(@PathVariable Long id,
                                                              Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.getRefundDetails(id, user));
    }
}
