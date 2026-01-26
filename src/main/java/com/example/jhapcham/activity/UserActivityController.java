package com.example.jhapcham.activity;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class UserActivityController {

    private final UserActivityService userActivityService;
    private final RecommendationService recommendationService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<UserActivityResponseDTO>> getUserActivities(@PathVariable Long userId) {
        return ResponseEntity.ok(userActivityService.getUserActivitiesWithProductNames(userId));
    }

    @GetMapping("/ibcf-data")
    public ResponseEntity<List<Map<String, Object>>> getIBCFData() {
        return ResponseEntity.ok(userActivityService.getInteractionsForIBCF());
    }

    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<com.example.jhapcham.product.ProductResponseDTO>> getRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(recommendationService.getRecommendations(userId, limit));
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncActivities() {
        userActivityService.syncActivitiesFromExistingData();
        return ResponseEntity.ok("Activities synchronized successfully");
    }

    @PostMapping("/record")
    public ResponseEntity<Void> recordActivity(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam ActivityType type,
            @RequestParam(required = false) String details) {
        userActivityService.recordActivity(userId, productId, type, details);
        return ResponseEntity.ok().build();
    }

    public record UserActivityResponseDTO(
            Long id,
            Long userId,
            Long productId,
            String productName,
            ActivityType activityType,
            String details,
            java.time.LocalDateTime timestamp) {
    }
}
