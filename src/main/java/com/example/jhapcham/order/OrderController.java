package com.example.jhapcham.order;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Single product order
    @PostMapping("/place")
    public ResponseEntity<?> placeOrderSingle(@RequestParam Long userId,
                                              @RequestParam Long productId,
                                              @RequestParam(defaultValue = "1") int quantity) {
        return orderService.placeOrderSingle(userId, productId, quantity);
    }

    // Checkout entire cart
    @PostMapping("/checkout")
    public ResponseEntity<?> placeOrderFromCart(@RequestParam Long userId) {
        return orderService.placeOrderFromCart(userId);
    }

    // User orders (PII-safe DTO)
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(@PathVariable Long userId) {
        return orderService.getUserOrders(userId);
    }

    // Seller orders
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<?> getSellerOrders(@PathVariable Long sellerId) {
        return orderService.getSellerOrders(sellerId);
    }

    // One order
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOne(@PathVariable Long orderId) {
        return orderService.getOne(orderId);
    }

    // Update status (restock on CANCELLED)
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long orderId,
                                          @RequestParam OrderStatus status) {
        return orderService.updateStatus(orderId, status);
    }

    // (Optional) status history endpoint if you decide to expose raw histories later
    // @GetMapping("/{orderId}/status-history") ...
}
