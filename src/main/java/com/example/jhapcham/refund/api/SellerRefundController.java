package com.example.jhapcham.refund.api;

import com.example.jhapcham.refund.application.RefundService;
import com.example.jhapcham.refund.dto.*;
import com.example.jhapcham.security.CurrentUserService;
import com.example.jhapcham.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/refunds")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerRefundController {

    private final RefundService refundService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<List<RefundResponseDTO>> getSellerRefunds(Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.getSellerRefunds(seller));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<RefundResponseDTO> approveRefund(@PathVariable Long id,
                                                           @RequestParam(value = "returnRequired", required = false, defaultValue = "true") boolean returnRequired,
                                                           Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.approveRefund(id, returnRequired, seller));
    }

    @PostMapping("/{id}/accept-negotiation")
    public ResponseEntity<RefundResponseDTO> acceptNegotiation(@PathVariable Long id,
                                                               Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.acceptNegotiation(id, seller));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<RefundResponseDTO> rejectRefund(@PathVariable Long id,
                                                          @RequestParam(value = "notes", required = false) String notes,
                                                          Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.rejectRefund(id, notes, seller));
    }

    @PostMapping("/{id}/escalate")
    public ResponseEntity<RefundResponseDTO> escalateRefund(@PathVariable Long id,
                                                            Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.sellerEscalate(id, seller));
    }

    @PostMapping("/{id}/request-evidence")
    public ResponseEntity<RefundResponseDTO> requestEvidence(@PathVariable Long id,
                                                             Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.requestEvidence(id, seller));
    }

    @PostMapping("/{id}/received")
    public ResponseEntity<RefundResponseDTO> confirmReturnReceived(@PathVariable Long id,
                                                                   Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.confirmReturnReceived(id, seller));
    }

    @PostMapping("/{id}/inspection")
    public ResponseEntity<RefundResponseDTO> performInspection(@PathVariable Long id,
                                                               @Valid @RequestBody InspectionRequestDTO dto,
                                                               Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.performInspection(id, dto, seller));
    }

    @PostMapping("/{id}/partial-refund")
    public ResponseEntity<RefundResponseDTO> offerPartialRefund(@PathVariable Long id,
                                                                @Valid @RequestBody PartialRefundRequestDTO req,
                                                                Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.offerPartialRefund(id, req, seller));
    }

    @PostMapping("/{id}/full-refund")
    public ResponseEntity<RefundResponseDTO> offerFullRefund(@PathVariable Long id,
                                                             @Valid @RequestBody FullRefundOfferRequestDTO req,
                                                             Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.offerFullRefund(id, req, seller));
    }

    @PostMapping("/{id}/exchange")
    public ResponseEntity<RefundResponseDTO> offerExchange(@PathVariable Long id,
                                                           @Valid @RequestBody ExchangeRequestDTO req,
                                                           Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.offerExchange(id, req, seller));
    }

    @PostMapping("/{id}/process-refund")
    public ResponseEntity<RefundResponseDTO> processRefundAfterInspection(@PathVariable Long id,
                                                                         Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.processRefundAfterInspection(id, seller));
    }

    @PostMapping("/{id}/process-exchange")
    public ResponseEntity<RefundResponseDTO> processExchangeAfterInspection(@PathVariable Long id,
                                                                           Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.processExchangeAfterInspection(id, seller));
    }

    @PostMapping("/{id}/ship-replacement")
    public ResponseEntity<RefundResponseDTO> shipReplacement(@PathVariable Long id,
                                                             @Valid @RequestBody ReplacementTrackingRequestDTO dto,
                                                             Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.shipReplacement(id, dto, seller));
    }

    @PostMapping("/{id}/submit-payout")
    public ResponseEntity<RefundResponseDTO> submitPayoutProof(@PathVariable Long id,
                                                               @Valid @RequestBody PayoutProofRequestDTO dto,
                                                               Authentication authentication) {
        User seller = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.submitPayoutProof(id, dto, seller));
    }
}
