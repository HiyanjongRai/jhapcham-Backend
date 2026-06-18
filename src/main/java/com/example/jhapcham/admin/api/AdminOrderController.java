package com.example.jhapcham.admin.api;


import com.example.jhapcham.admin.application.*;
import com.example.jhapcham.admin.dto.*;
import com.example.jhapcham.order.dto.OrderSummaryDTO;
import com.example.jhapcham.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
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

    @PutMapping("/{orderId}/deliver-manually")
    public ResponseEntity<Void> deliverManually(@PathVariable Long orderId) {
        adminService.manuallyDeliverOrder(orderId);
        return ResponseEntity.ok().build();
    }
}
