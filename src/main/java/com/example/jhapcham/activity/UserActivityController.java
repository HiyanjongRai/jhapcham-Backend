// src/main/java/com/example/jhapcham/activity/UserActivityController.java
package com.example.jhapcham.activity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/users/{userId}/activities")
@RequiredArgsConstructor
public class UserActivityController {
    private final ActivityAggregatorService aggregator;
    @GetMapping
    public ResponseEntity<ActivityAggregatorService.PagedActivity> list(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Integer lastNDays
    ) {
        var out = aggregator.fetchUserFeed(userId, page, Math.min(size, 200), lastNDays);
        return ResponseEntity.ok(out);
    }
}
