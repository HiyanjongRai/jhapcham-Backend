package com.example.jhapcham.activity.api;


import com.example.jhapcham.activity.application.*;
import com.example.jhapcham.activity.domain.*;
import com.example.jhapcham.activity.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class UserActivityController {

    private final UserActivityService userActivityService;
    private final RecommendationService recommendationService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<UserActivityResponseDTO>> getUserActivities(
            @PathVariable Long userId,
            Authentication authentication) {
        com.example.jhapcham.user.domain.User actor = currentUserService.requireUser(authentication);
        currentUserService.requireSelfOrAdmin(actor, userId);
        return ResponseEntity.ok(userActivityService.getUserActivitiesWithProductNames(userId));
    }

    @GetMapping("/ibcf-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getIBCFData() {
        return ResponseEntity.ok(userActivityService.getInteractionsForIBCF());
    }

    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<com.example.jhapcham.product.dto.ProductResponseDTO>> getRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        com.example.jhapcham.user.domain.User actor = currentUserService.requireUser(authentication);
        currentUserService.requireSelfOrAdmin(actor, userId);
        return ResponseEntity.ok(recommendationService.getRecommendations(userId, limit));
    }

    @GetMapping("/similar/{productId}")
    public ResponseEntity<List<com.example.jhapcham.product.dto.ProductResponseDTO>> getSimilarItems(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(recommendationService.getSimilarProducts(productId, limit));
    }

    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testIBCF(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long productId) {

        Map<String, Object> testResults = new java.util.LinkedHashMap<>();

        // 1. All IBCF Data Summary
        List<Map<String, Object>> allIbcfData = userActivityService.getInteractionsForIBCF();
        testResults.put("totalInteractionsStored", allIbcfData.size());
        if (!allIbcfData.isEmpty()) {
            testResults.put("sampleInteraction", allIbcfData.get(0));
            // Add top 5 raw activities
            testResults.put("rawIbcfDataPreview", allIbcfData.stream().limit(5).toList());
        }

        // 2. User specific tests
        if (userId != null) {
            testResults.put("targetUserId", userId);
            testResults.put("userActivities", userActivityService.getUserActivitiesWithProductNames(userId));
            testResults.put("recommendationsForUser", recommendationService.getRecommendations(userId, 5));
        }

        // 3. Product specific tests
        if (productId != null) {
            testResults.put("targetProductId", productId);
            testResults.put("similarProductsToTarget", recommendationService.getSimilarProducts(productId, 5));
        }

        return ResponseEntity.ok(testResults);
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> syncActivities() {
        userActivityService.syncActivitiesFromExistingData();
        return ResponseEntity.ok("Activities synchronized successfully");
    }

    @PostMapping("/record")
    public ResponseEntity<Void> recordActivity(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam ActivityType type,
            @RequestParam(required = false) String details,
            Authentication authentication) {
        com.example.jhapcham.user.domain.User actor = currentUserService.requireUser(authentication);
        currentUserService.requireSelfOrAdmin(actor, userId);
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
