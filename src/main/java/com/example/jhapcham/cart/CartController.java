package com.example.jhapcham.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestParam Long userId,
                                       @RequestParam Long productId,
                                       @RequestParam int quantity) {
        try {
            cartService.addToCart(userId, productId, quantity);
            return ResponseEntity.ok(Map.of("message", "Item added to cart"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyCart(@RequestParam Long userId) {
        try {
            List<CartItemDto> userCart = cartService.getCartItems(userId);
            // Optional: include a cart total
            double total = userCart.stream()
                    .mapToDouble(i -> i.getLineTotal() == null ? 0.0 : i.getLineTotal())
                    .sum();
            return ResponseEntity.ok(Map.of("items", userCart, "total", total));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/quantity")
    public ResponseEntity<?> updateQuantity(@RequestParam Long userId,
                                            @RequestParam Long productId,
                                            @RequestParam int quantity) {
        try {
            cartService.updateQuantity(userId, productId, quantity);
            return ResponseEntity.ok(Map.of("message", "Quantity updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeFromCart(@RequestParam Long userId,
                                            @RequestParam Long productId) {
        try {
            cartService.removeCartItem(userId, productId);
            return ResponseEntity.ok(Map.of("message", "Item removed from cart"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clear(@RequestParam Long userId) {
        try {
            cartService.clearCart(userId);
            return ResponseEntity.ok(Map.of("message", "Cart cleared"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
