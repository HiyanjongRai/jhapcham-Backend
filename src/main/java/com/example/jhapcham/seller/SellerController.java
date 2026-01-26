package com.example.jhapcham.seller;

import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    // Get full seller profile + products
    @GetMapping("/{sellerUserId}")
    public ResponseEntity<?> getSeller(@PathVariable Long sellerUserId) {
        return ResponseEntity.ok(sellerService.getSellerProfile(sellerUserId));
    }

    // Update seller profile + logo
    @PutMapping("/{sellerUserId}")
    public ResponseEntity<SellerProfileResponseDTO> updateSeller(
            @PathVariable Long sellerUserId,
            @ModelAttribute SellerUpdateRequestDTO dto) {
        return ResponseEntity.ok(sellerService.updateSeller(sellerUserId, dto));
    }

    @GetMapping("/{sellerUserId}/income")
    public ResponseEntity<?> getSellerIncome(@PathVariable Long sellerUserId) {
        try {
            return ResponseEntity.ok(
                    sellerService.getSellerIncome(sellerUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Failed to load seller income"));

        }
    }

    @GetMapping("/{sellerUserId}/dashboard")
    public ResponseEntity<?> getDashboardStats(@PathVariable Long sellerUserId) {
        try {
            return ResponseEntity.ok(
                    sellerService.getDashboardStats(sellerUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Failed to load dashboard statistics"));
        }
    }
}