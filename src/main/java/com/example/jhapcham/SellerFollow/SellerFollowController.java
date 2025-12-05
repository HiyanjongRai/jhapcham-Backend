package com.example.jhapcham.SellerFollow;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class SellerFollowController {

    private final SellerFollowService followService;

    @PostMapping("/{customerId}/follow/{sellerId}")
    public ResponseEntity<String> followSeller(
            @PathVariable Long customerId,
            @PathVariable Long sellerId
    ) {
        return ResponseEntity.ok(followService.followSeller(customerId, sellerId));
    }

    @GetMapping("/{customerId}/is-following/{sellerId}")
    public ResponseEntity<Boolean> isFollowing(
            @PathVariable Long customerId,
            @PathVariable Long sellerId
    ) {
        return ResponseEntity.ok(followService.isFollowing(customerId, sellerId));
    }

    @DeleteMapping("/{customerId}/unfollow/{sellerId}")
    public ResponseEntity<String> unfollowSeller(
            @PathVariable Long customerId,
            @PathVariable Long sellerId
    ) {
        return ResponseEntity.ok(followService.unfollowSeller(customerId, sellerId));
    }

    @GetMapping("/list/{customerId}")
    public ResponseEntity<List<FollowedSellerDTO>> getFollowedSellers(@PathVariable Long customerId) {
        return ResponseEntity.ok(followService.getAllFollowedSellers(customerId));
    }
}
