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
}
