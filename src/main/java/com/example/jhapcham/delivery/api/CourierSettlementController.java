package com.example.jhapcham.delivery.api;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settlements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CourierSettlementController {

    private final CourierSettlementService settlementService;

    /**
     * Admin: View all settlement records for a courier (full history)
     */
    @GetMapping("/courier/{courierId}")
    public ResponseEntity<?> getCourierSettlements(@PathVariable Long courierId) {
        try {
            return ResponseEntity.ok(settlementService.getSettlementsForCourier(courierId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Admin: Get a courier's current cash-in-hand summary (how much they owe the hub)
     */
    @GetMapping("/courier/{courierId}/summary")
    public ResponseEntity<?> getCourierCashSummary(@PathVariable Long courierId) {
        try {
            return ResponseEntity.ok(settlementService.getCourierCashSummary(courierId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Admin: Mark a single settlement record as remitted after physically receiving cash
     */
    @PutMapping("/{settlementId}/remit")
    public ResponseEntity<?> markRemitted(
            @PathVariable Long settlementId,
            @RequestBody(required = false) CourierSettlementDTO.RemittanceRequest request) {
        try {
            return ResponseEntity.ok(settlementService.markRemitted(settlementId, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Admin: Bulk-remit all pending COD cash from a courier at end of shift
     * Triggers seller payout for all associated COD orders in one action.
     */
    @PutMapping("/courier/{courierId}/bulk-remit")
    public ResponseEntity<?> bulkRemit(
            @PathVariable Long courierId,
            @RequestBody(required = false) CourierSettlementDTO.RemittanceRequest request) {
        try {
            int count = settlementService.bulkRemitCourier(courierId, request);
            return ResponseEntity.ok("Successfully remitted " + count + " COD settlement(s).");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
