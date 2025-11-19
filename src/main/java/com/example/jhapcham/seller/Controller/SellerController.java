package com.example.jhapcham.seller.Controller;

import com.example.jhapcham.seller.Service.SellerService;
import com.example.jhapcham.seller.Service.SellerProfileStatsService;
import com.example.jhapcham.seller.dto.SellerProfileStatsDto;
import com.example.jhapcham.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;
    private final SellerProfileStatsService sellerProfileStatsService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerSeller(@RequestBody User seller) {
        User savedSeller = sellerService.createSeller(seller);
        Map<String, Object> response = Map.of(
                "message", "Seller registered successfully! Waiting for admin approval.",
                "userId", savedSeller.getId()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String usernameOrEmail = body.get("usernameOrEmail");
        String password = body.get("password");

        if (usernameOrEmail == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "usernameOrEmail and password are required"));
        }

        try {
            User user = sellerService.authenticateSeller(usernameOrEmail, password);
            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "userId", user.getId(),
                    "username", user.getUsername()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{sellerUserId}/profile-stats")
    public ResponseEntity<SellerProfileStatsDto> getSellerProfileStats(@PathVariable Long sellerUserId) {
        SellerProfileStatsDto stats = sellerProfileStatsService.getSellerProfileStats(sellerUserId);
        return ResponseEntity.ok(stats);
    }
}
