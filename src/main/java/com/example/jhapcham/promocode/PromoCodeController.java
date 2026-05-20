package com.example.jhapcham.promocode;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promos") // Changed from /api/seller/promos to avoid conflict with /api/seller/{id}
@RequiredArgsConstructor
public class PromoCodeController {

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

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<PromoCode>> getSellerPromos(@PathVariable Long sellerId, Authentication authentication) {
        currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerId);
        return ResponseEntity.ok(promoCodeService.getSellerPromos(sellerId));
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
            } catch (Exception ignored) {}
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
        private List<com.example.jhapcham.order.CheckoutItemDTO> items;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class PromoCodeValidationResponse {
        private boolean valid;
        private java.math.BigDecimal discountAmount;
        private Long applicableSellerId;
        private com.example.jhapcham.campaign.DiscountType discountType;
        private java.math.BigDecimal discountValue;
        private String message;
    }
}
