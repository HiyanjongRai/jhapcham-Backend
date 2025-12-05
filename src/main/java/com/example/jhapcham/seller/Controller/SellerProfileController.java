package com.example.jhapcham.seller.Controller;

import com.example.jhapcham.seller.Service.SellerProfileService;
import com.example.jhapcham.seller.dto.SellerProfileRequestDTO;
import com.example.jhapcham.seller.dto.SellerProfileResponseDTO;
import com.example.jhapcham.seller.model.SellerProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/seller-profiles")
@RequiredArgsConstructor
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    @PostMapping("/{userId}")
    public ResponseEntity<SellerProfileResponseDTO> createOrUpdateProfile(
            @PathVariable Long userId,
            @ModelAttribute SellerProfileRequestDTO dto
    ) {
        try {
            SellerProfile profile = sellerProfileService.createOrUpdateProfile(userId, dto);
            return sellerProfileService.getSellerProfileWithProducts(userId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<SellerProfileResponseDTO> getSellerProfile(@PathVariable Long userId) {
        return sellerProfileService.getSellerProfileWithProducts(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
