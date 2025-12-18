package com.example.jhapcham.seller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller-profiles")
@RequiredArgsConstructor
public class SellerProfileController {

    private final SellerService sellerService;

    @GetMapping("/{id}")
    public ResponseEntity<SellerProfileResponseDTO> getSellerProfile(@PathVariable Long id) {
        // Here id is sellerUserId based on how frontend calls it.
        // Wait, frontend calls /api/seller-profiles/{id} with sellerId (User ID of
        // seller).
        // My previous investigation said product.sellerId is userId.

        return ResponseEntity.ok(sellerService.getSellerProfile(id));
    }
}
