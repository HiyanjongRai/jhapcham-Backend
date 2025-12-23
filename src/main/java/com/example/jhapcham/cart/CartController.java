package com.example.jhapcham.cart;

import com.example.jhapcham.cart.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @PostMapping("/{userId}/add/{productId}")
    public ResponseEntity<?> addToCart(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestBody AddToCartRequestDTO dto
    ) {
        try {
            CartResponseDTO cart =
                    cartService.addToCart(userId, productId, dto);

            return ResponseEntity.ok(cart);

        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body("Unable to add item to cart");
        }


    }

    @PutMapping("/{userId}/update/{cartItemId}")
    public ResponseEntity<?> updateQuantity(
            @PathVariable Long userId,
            @PathVariable Long cartItemId,
            @RequestParam Integer qty
    ) {
        try {
            CartResponseDTO cart = cartService.updateQuantity(userId, cartItemId, qty);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to update cart item");
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Long userId) {
        try {
            CartResponseDTO cart = cartService.getCart(userId);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch cart");
        }
    }

}