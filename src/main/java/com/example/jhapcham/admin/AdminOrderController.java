package com.example.jhapcham.admin;

import com.example.jhapcham.order.OrderSummaryDTO;
import com.example.jhapcham.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<List<OrderSummaryDTO>> getAllOrders() {
        return ResponseEntity.ok(adminService.getAllOrders());
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(@PathVariable Long orderId, @RequestParam OrderStatus status) {
        adminService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok().build();
    }
}
