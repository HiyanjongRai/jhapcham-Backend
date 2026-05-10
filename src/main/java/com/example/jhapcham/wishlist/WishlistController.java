package com.example.jhapcham.wishlist;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @PostMapping("/{userId}/{productId}")
    public ResponseEntity<?> addToWishlist(
            @PathVariable Long userId,
            @PathVariable Long productId,
            Authentication authentication
    ) {
        currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
        wishlistService.addToWishlist(userId, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/{productId}")
    public ResponseEntity<?> removeFromWishlist(
            @PathVariable Long userId,
            @PathVariable Long productId,
            Authentication authentication
    ) {
        currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getWishlist(@PathVariable Long userId, Authentication authentication) {
        currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
        return ResponseEntity.ok(wishlistService.getWishlist(userId));
    }

    @GetMapping("/{userId}/check/{productId}")
    public ResponseEntity<?> isInWishlist(
            @PathVariable Long userId,
            @PathVariable Long productId,
            Authentication authentication
    ) {
        currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
        return ResponseEntity.ok(
                wishlistService.isInWishlist(userId, productId)
        );
    }
}
