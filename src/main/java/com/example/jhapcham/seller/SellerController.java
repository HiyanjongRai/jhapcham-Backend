package com.example.jhapcham.seller;

import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.order.CommissionStatus;
import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;
    private final OrderRepository orderRepository;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @GetMapping("/{sellerUserId}")
    public ResponseEntity<?> getSeller(@PathVariable Long sellerUserId) {
        return ResponseEntity.ok(sellerService.getSellerProfile(sellerUserId));
    }

    @PutMapping("/{sellerUserId}")
    public ResponseEntity<SellerProfileResponseDTO> updateSeller(
            @PathVariable Long sellerUserId,
            @ModelAttribute SellerUpdateRequestDTO dto,
            Authentication authentication) {
        currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerUserId);
        return ResponseEntity.ok(sellerService.updateSeller(sellerUserId, dto));
    }

    @GetMapping("/{sellerUserId}/income")
    public ResponseEntity<?> getSellerIncome(@PathVariable Long sellerUserId, Authentication authentication) {
        try {
            currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerUserId);
            return ResponseEntity.ok(sellerService.getSellerIncome(sellerUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{sellerUserId}/stats")
    public ResponseEntity<?> getDashboardStats(@PathVariable Long sellerUserId, Authentication authentication) {
        currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerUserId);
        return ResponseEntity.ok(sellerService.getDashboardStats(sellerUserId));
    }

    @GetMapping("/{sellerUserId}/commissions")
    public ResponseEntity<?> getSellerCommissions(@PathVariable Long sellerUserId, Authentication authentication) {
        currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerUserId);
        return ResponseEntity.ok(sellerService.getSellerCommissions(sellerUserId));
    }

    @PostMapping("/{sellerUserId}/commissions/{orderId}/pay")
    public ResponseEntity<?> payCommission(@PathVariable Long sellerUserId, @PathVariable Long orderId,
            Authentication authentication) {
        currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerUserId);
        return ResponseEntity.status(403)
                .body(new ErrorResponse("Commission settlement must be verified by admin or payment reconciliation."));
    }

    @PostMapping("/{sellerUserId}/commissions/pay-all")
    public ResponseEntity<?> payAllCommissions(@PathVariable Long sellerUserId, Authentication authentication) {
        currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerUserId);
        return ResponseEntity.status(403)
                .body(new ErrorResponse("Bulk commission settlement must be verified by admin or payment reconciliation."));
    }
}
