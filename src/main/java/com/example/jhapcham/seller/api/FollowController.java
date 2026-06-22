package com.example.jhapcham.seller.api;


import com.example.jhapcham.common.ApiResponse;
import com.example.jhapcham.seller.application.*;
import com.example.jhapcham.seller.domain.*;
import com.example.jhapcham.seller.dto.*;
import com.example.jhapcham.seller.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @PostMapping("/{userId}/follow/{sellerId}")
    public ResponseEntity<ApiResponse<String>> follow(@PathVariable Long userId, @PathVariable Long sellerId,
            Authentication authentication) {
        currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
        return ResponseEntity.ok(ApiResponse.success("Seller followed successfully", followService.followSeller(userId, sellerId)));
    }

    @DeleteMapping("/{userId}/unfollow/{sellerId}")
    public ResponseEntity<ApiResponse<String>> unfollow(@PathVariable Long userId, @PathVariable Long sellerId,
            Authentication authentication) {
        currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
        return ResponseEntity.ok(ApiResponse.success("Seller unfollowed successfully", followService.unfollowSeller(userId, sellerId)));
    }

    @GetMapping("/{userId}/is-following/{sellerId}")
    public ResponseEntity<Boolean> isFollowing(@PathVariable Long userId, @PathVariable Long sellerId,
            Authentication authentication) {
        currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
        return ResponseEntity.ok(followService.isFollowing(userId, sellerId));
    }

    @GetMapping("/{userId}/followed-sellers")
    public ResponseEntity<java.util.List<SellerProfileResponseDTO>> getFollowedSellers(@PathVariable Long userId,
            Authentication authentication) {
        currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
        return ResponseEntity.ok(followService.getFollowedSellers(userId));
    }
}
