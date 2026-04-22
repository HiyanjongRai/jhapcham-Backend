package com.example.jhapcham.loyalty;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
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
    private final UserRepository userRepository;

    @GetMapping("/my-points")
    public ResponseEntity<?> getMyLoyaltyPoints(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Please login to view loyalty points"));
            }
            String principal = authentication.getName();
            User user = userRepository.findByUsername(principal)
                    .or(() -> userRepository.findByEmail(principal))
                    .orElseThrow(() -> new RuntimeException("User not found"));

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
            if (authentication == null || authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Please login to redeem points"));
            }
            String principal = authentication.getName();
            User user = userRepository.findByUsername(principal)
                    .or(() -> userRepository.findByEmail(principal))
                    .orElseThrow(() -> new RuntimeException("User not found"));

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
