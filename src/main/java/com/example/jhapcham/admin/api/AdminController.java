package com.example.jhapcham.admin.api;


import com.example.jhapcham.admin.application.*;
import com.example.jhapcham.admin.dto.*;
import com.example.jhapcham.product.dto.ProductResponseDTO;
import com.example.jhapcham.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AdminAuditService adminAuditService;

    // Analytics / Overview
    @GetMapping("/analytics")
    public ResponseEntity<PlatformAnalyticsDTO> getPlatformAnalytics() {
        return ResponseEntity.ok(adminService.getPlatformAnalytics());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AdminAuditLogDTO>> getAuditLogs(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(adminAuditService.search(
                q,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/commissions")
    public ResponseEntity<List<CommissionReportDTO>> getCommissionReports() {
        return ResponseEntity.ok(adminService.getCommissionReports());
    }

    @PostMapping("/commissions/{orderId}/remind")
    public ResponseEntity<Void> sendCommissionReminder(@PathVariable Long orderId) {
        adminService.sendCommissionReminder(orderId);
        return ResponseEntity.ok().build();
    }

    // Users
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{userId}/block")
    public ResponseEntity<Void> blockUser(@PathVariable Long userId) {
        adminService.blockUser(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/unblock")
    public ResponseEntity<Void> unblockUser(@PathVariable Long userId) {
        adminService.unblockUser(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<CustomerAdminDetailDTO> getCustomerDetails(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getCustomerDetails(userId));
    }

    @GetMapping("/sellers")
    public ResponseEntity<List<SellerAdminDetailDTO>> getAllSellers() {
        return ResponseEntity.ok(adminService.getAllSellersMetrics());
    }

    @GetMapping("/sellers/{sellerId}")
    public ResponseEntity<SellerAdminDetailDTO> getSellerDetails(@PathVariable Long sellerId) {
        return ResponseEntity
                .ok(adminService.getSellerDetails(Objects.requireNonNull(sellerId, "Seller ID cannot be null")));
    }

    // Products
    @GetMapping("/products")
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return ResponseEntity.ok(adminService.getAllProducts(
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"))));
    }

    @PutMapping("/products/{productId}/visibility")
    public ResponseEntity<Void> setProductVisibility(
            @PathVariable Long productId,
            @RequestParam boolean visible) {
        adminService.setProductVisibility(productId, visible);
        return ResponseEntity.ok().build();
    }

    // Pending Seller Applications
    @GetMapping("/sellers/applications/pending")
    public ResponseEntity<List<com.example.jhapcham.seller.domain.SellerApplication>> getPendingApplications() {
        return ResponseEntity.ok(adminService.getPendingApplications());
    }

    @GetMapping("/sellers/{sellerId}/orders")
    public ResponseEntity<List<com.example.jhapcham.order.dto.OrderSummaryDTO>> getSellerOrders(@PathVariable Long sellerId) {
        return ResponseEntity.ok(adminService.getOrdersBySeller(sellerId));
    }

    @PostMapping("/sellers/applications/{appId}/approve")
    public ResponseEntity<Void> approveApplication(
            @PathVariable Long appId,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String note = (body != null && body.containsKey("note")) ? body.get("note") : "Approved by admin";
        adminService.approveSellerApplication(appId, note);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sellers/applications/{appId}/reject")
    public ResponseEntity<Void> rejectApplication(
            @PathVariable Long appId,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String note = (body != null && body.containsKey("note")) ? body.get("note") : "Rejected by admin";
        adminService.rejectSellerApplication(appId, note);
        return ResponseEntity.ok().build();
    }
}
