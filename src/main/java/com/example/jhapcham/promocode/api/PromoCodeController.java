package com.example.jhapcham.promocode.api;


import com.example.jhapcham.promocode.application.*;
import com.example.jhapcham.promocode.domain.*;
import com.example.jhapcham.promocode.dto.*;
import com.example.jhapcham.promocode.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/promos") // Changed from /api/seller/promos to avoid conflict with /api/seller/{id}
@RequiredArgsConstructor
public class PromoCodeController {

    private static final Logger logger = LoggerFactory.getLogger(PromoCodeController.class);

    private final PromoCodeService promoCodeService;
    private final PromoCodeRepository promoCodeRepository;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<PromoCodeDTO> createPromo(@RequestBody PromoCodeDTO dto, Authentication authentication) {
        var actor = currentUserService.requireUser(authentication);
        if (dto.getSellerId() == null) {
            currentUserService.requireAdmin(actor);
        } else {
            currentUserService.requireSellerSelfOrAdmin(actor, dto.getSellerId());
        }
        return ResponseEntity.ok(promoCodeService.createPromoCode(dto));
    }

    /** Admin: list every promo code in the system. */
    @GetMapping
    public ResponseEntity<List<com.example.jhapcham.promocode.domain.PromoCode>> getAllPromos(Authentication authentication) {
        currentUserService.requireAdmin(currentUserService.requireUser(authentication));
        return ResponseEntity.ok(promoCodeService.getAllPromoCodes());
    }

    /** Admin / owner: fetch one promo code by ID. */
    @GetMapping("/{id}")
    public ResponseEntity<com.example.jhapcham.promocode.domain.PromoCode> getPromoById(
            @PathVariable Long id, Authentication authentication) {
        currentUserService.requireAdmin(currentUserService.requireUser(authentication));
        return ResponseEntity.ok(promoCodeService.getPromoCodeById(id));
    }

    /** Admin / owner: update mutable fields of a promo code. */
    @PutMapping("/{id}")
    public ResponseEntity<PromoCodeDTO> updatePromo(
            @PathVariable Long id,
            @RequestBody PromoCodeDTO dto,
            Authentication authentication) {
        var actor = currentUserService.requireUser(authentication);
        com.example.jhapcham.promocode.domain.PromoCode existing = promoCodeService.getPromoCodeById(id);
        if (existing.getSellerId() != null) {
            currentUserService.requireSellerSelfOrAdmin(actor, existing.getSellerId());
        } else {
            currentUserService.requireAdmin(actor);
        }
        return ResponseEntity.ok(promoCodeService.updatePromoCode(id, dto));
    }

    /** Admin: toggle active/inactive status. */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<com.example.jhapcham.promocode.domain.PromoCode> togglePromo(
            @PathVariable Long id, Authentication authentication) {
        currentUserService.requireAdmin(currentUserService.requireUser(authentication));
        return ResponseEntity.ok(promoCodeService.togglePromoCode(id));
    }

    /** Admin: permanently delete a promo code. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromo(@PathVariable Long id, Authentication authentication) {
        currentUserService.requireAdmin(currentUserService.requireUser(authentication));
        promoCodeService.deletePromoCode(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<com.example.jhapcham.promocode.domain.PromoCode>> getSellerPromos(
            @PathVariable Long sellerId, Authentication authentication) {
        currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerId);
        return ResponseEntity.ok(promoCodeService.getSellerPromos(sellerId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<PromoCode>> getActivePromos() {
        List<PromoCode> active = promoCodeService.getAllPromoCodes().stream()
                .filter(PromoCode::isValid)
                .toList();
        return ResponseEntity.ok(active);
    }

    @PostMapping("/validate")
    public ResponseEntity<PromoCodeValidationResponse> validatePromo(
            @RequestBody PromoCodeValidationRequest request,
            Authentication authentication) {
        
        Long userId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                var actor = currentUserService.requireUser(authentication);
                if (actor != null) {
                    userId = actor.getId();
                }
            } catch (Exception ex) {
                logger.debug("Skipping authenticated promo validation context", ex);
            }
        }
        
        try {
            PromoCodeService.DiscountResult result = promoCodeService.calculateDiscount(
                    request.getCode(), 
                    request.getItems(), 
                    userId
            );
            
            if (result.getAmount().compareTo(java.math.BigDecimal.ZERO) == 0) {
                return ResponseEntity.ok(PromoCodeValidationResponse.builder()
                        .valid(false)
                        .discountAmount(java.math.BigDecimal.ZERO)
                        .message("❌ This coupon is not applicable to any items in your cart.")
                        .build());
            }

            PromoCode promo = promoCodeRepository.findByCode(request.getCode().trim().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid promo code"));
            
            return ResponseEntity.ok(PromoCodeValidationResponse.builder()
                    .valid(true)
                    .discountAmount(result.getAmount())
                    .applicableSellerId(result.getApplicableSellerId())
                    .discountType(promo.getDiscountType())
                    .discountValue(promo.getDiscountValue())
                    .message("🎟️ Coupon applied successfully!")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(PromoCodeValidationResponse.builder()
                    .valid(false)
                    .discountAmount(java.math.BigDecimal.ZERO)
                    .message("❌ " + e.getMessage())
                    .build());
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class PromoCodeValidationRequest {
        private String code;
        private List<com.example.jhapcham.order.dto.CheckoutItemDTO> items;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class PromoCodeValidationResponse {
        private boolean valid;
        private java.math.BigDecimal discountAmount;
        private Long applicableSellerId;
        private com.example.jhapcham.campaign.domain.DiscountType discountType;
        private java.math.BigDecimal discountValue;
        private String message;
    }
}
