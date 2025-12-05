package com.example.jhapcham.wishlist;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/add")
    public ResponseEntity<?> add(
            @RequestParam Long userId,
            @RequestParam Long productId
    ) {
        wishlistService.add(userId, productId);
        return ResponseEntity.ok("Added");
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> remove(
            @RequestParam Long userId,
            @RequestParam Long productId
    ) {
        wishlistService.remove(userId, productId);
        return ResponseEntity.ok("Removed");
    }

    @GetMapping
    public ResponseEntity<List<WishlistItemDTO>> get(
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(wishlistService.getAll(userId));
    }

    @PostMapping("/toggle")
    public ResponseEntity<String> toggle(
            @RequestParam Long userId,
            @RequestParam Long productId
    ) {
        boolean added = wishlistService.toggle(userId, productId);
        return ResponseEntity.ok(added ? "ADDED" : "REMOVED");
    }
}
