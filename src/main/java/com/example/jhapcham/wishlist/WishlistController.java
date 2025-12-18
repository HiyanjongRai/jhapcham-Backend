package com.example.jhapcham.wishlist;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{userId}/{productId}")
    public ResponseEntity<?> addToWishlist(
            @PathVariable Long userId,
            @PathVariable Long productId
    ) {
        wishlistService.addToWishlist(userId, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/{productId}")
    public ResponseEntity<?> removeFromWishlist(
            @PathVariable Long userId,
            @PathVariable Long productId
    ) {
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getWishlist(@PathVariable Long userId) {
        return ResponseEntity.ok(wishlistService.getWishlist(userId));
    }

    @GetMapping("/{userId}/check/{productId}")
    public ResponseEntity<?> isInWishlist(
            @PathVariable Long userId,
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(
                wishlistService.isInWishlist(userId, productId)
        );
    }
}
