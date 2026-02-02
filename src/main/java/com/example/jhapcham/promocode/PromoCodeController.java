package com.example.jhapcham.promocode;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promos") // Changed from /api/seller/promos to avoid conflict with /api/seller/{id}
@RequiredArgsConstructor
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @PostMapping
    public ResponseEntity<PromoCodeDTO> createPromo(@RequestBody PromoCodeDTO dto) {
        return ResponseEntity.ok(promoCodeService.createPromoCode(dto));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<PromoCode>> getSellerPromos(@PathVariable Long sellerId) {
        return ResponseEntity.ok(promoCodeService.getSellerPromos(sellerId));
    }
}
