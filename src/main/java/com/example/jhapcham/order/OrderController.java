package com.example.jhapcham.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/place")
    public ResponseEntity<?> placeOrder(@RequestParam Long userId, @RequestParam Long productId) {
        return orderService.placeOrder(userId, productId);
    }

    @PostMapping("/place-multi")
    public ResponseEntity<?> placeMultiOrder(@RequestParam Long userId, @RequestBody List<Long> productIds) {
        return orderService.placeMultiProductOrder(userId, productIds);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(@PathVariable Long userId) {
        return orderService.getUserOrders(userId);
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<?> getSellerOrders(@PathVariable Long sellerId) {
        return orderService.getSellerOrders(sellerId);
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long orderId, @RequestParam OrderStatus status) {
        return orderService.updateStatus(orderId, status);
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        return orderService.cancelOrder(orderId);
    }

    @GetMapping("/{orderId}/total")
    public ResponseEntity<?> getTotal(@PathVariable Long orderId) {
        return orderService.getOrderTotal(orderId);
    }

    @GetMapping("/{orderId}/status-history")
    public ResponseEntity<?> getStatusHistory(@PathVariable Long orderId) {
        return orderService.getStatusHistory(orderId);
    }

    @GetMapping("/seller/{id}/stats")
    public ResponseEntity<?> getSellerStats(@PathVariable Long id) {
        return orderService.getSellerStats(id);
    }

    @GetMapping("/report/monthly")
    public ResponseEntity<?> getMonthlyReport() {
        return orderService.getMonthlyReport();
    }
}
