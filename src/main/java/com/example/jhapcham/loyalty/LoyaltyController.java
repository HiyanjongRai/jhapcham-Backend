package com.example.jhapcham.loyalty;

import com.example.jhapcham.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @GetMapping("/my-points")
    public ResponseEntity<?> getMyLoyaltyPoints(Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);

            LoyaltyPointsDTO points = loyaltyService.getUserLoyaltyPoints(user.getId());
            return ResponseEntity.ok(points);
        } catch (Exception e) {
            log.error("Error fetching loyalty points: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/redeem")
    public ResponseEntity<?> redeemPoints(
            @RequestBody Map<String, Long> body,
            Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);

            Long pointsToRedeem = body.get("points");

            if (pointsToRedeem == null || pointsToRedeem <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid points amount"));
            }

            LoyaltyPointsDTO result = loyaltyService.redeemPoints(user.getId(), pointsToRedeem);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error redeeming loyalty points: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
