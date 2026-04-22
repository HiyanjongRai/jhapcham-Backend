package com.example.jhapcham.refund;

import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final UserRepository userRepository;

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getSellerRefunds(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
            return ResponseEntity.ok(refundService.getSellerRefunds(user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/admin/pending-payouts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingPayouts() {
        try {
            return ResponseEntity.ok(refundService.getPendingPayouts());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/admin/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> completeRefund(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String adminNotes = body.getOrDefault("adminNotes", "Payout processed by system");
            return ResponseEntity.ok(refundService.completeRefund(id, adminNotes));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
