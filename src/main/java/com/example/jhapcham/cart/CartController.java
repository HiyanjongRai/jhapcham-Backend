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
    public ResponseEntity<?> add(@RequestBody CartAddRequest req) {
        cartService.addToCart(req.getUserId(), req.getProductId(), req.getQuantity(), req.getColor(), req.getStorage());
        return ResponseEntity.ok(Map.of("message", "Item added"));
    }

    @GetMapping
    public ResponseEntity<?> get(@RequestParam Long userId) {
        List<CartItemDto> cart = cartService.getCartItems(userId);
        double total = cart.stream().mapToDouble(i -> i.getLineTotal()).sum();
        return ResponseEntity.ok(Map.of("items", cart, "total", total));
    }

    @PatchMapping("/quantity")
    public ResponseEntity<?> update(@RequestBody CartAddRequest req) {
        cartService.updateQuantity(req.getUserId(), req.getProductId(), req.getQuantity(), req.getColor(), req.getStorage());
        return ResponseEntity.ok(Map.of("message", "Quantity updated"));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> remove(@RequestBody CartAddRequest req) {
        cartService.removeCartItem(req.getUserId(), req.getProductId(), req.getColor(), req.getStorage());
        return ResponseEntity.ok(Map.of("message", "Item removed"));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clear(@RequestParam Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(Map.of("message", "Cart cleared"));
    }
}
