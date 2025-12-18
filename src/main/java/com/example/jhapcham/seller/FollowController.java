package com.example.jhapcham.seller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}/follow/{sellerId}")
    public ResponseEntity<String> follow(@PathVariable Long userId, @PathVariable Long sellerId) {
        return ResponseEntity.ok(followService.followSeller(userId, sellerId));
    }

    @DeleteMapping("/{userId}/unfollow/{sellerId}")
    public ResponseEntity<String> unfollow(@PathVariable Long userId, @PathVariable Long sellerId) {
        return ResponseEntity.ok(followService.unfollowSeller(userId, sellerId));
    }

    @GetMapping("/{userId}/is-following/{sellerId}")
    public ResponseEntity<Boolean> isFollowing(@PathVariable Long userId, @PathVariable Long sellerId) {
        return ResponseEntity.ok(followService.isFollowing(userId, sellerId));
    }
}
