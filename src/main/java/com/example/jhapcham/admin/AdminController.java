package com.example.jhapcham.admin;

import com.example.jhapcham.product.ProductResponseDTO;
import com.example.jhapcham.report.ReportDTO;
import com.example.jhapcham.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

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

    @GetMapping("/sellers/{sellerId}")
    public ResponseEntity<SellerAdminDetailDTO> getSellerDetails(@PathVariable Long sellerId) {
        return ResponseEntity.ok(adminService.getSellerDetails(sellerId));
    }

    // Products
    @GetMapping("/products")
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        return ResponseEntity.ok(adminService.getAllProducts());
    }

    @PutMapping("/products/{productId}/visibility")
    public ResponseEntity<Void> setProductVisibility(
            @PathVariable Long productId,
            @RequestParam boolean visible) {
        adminService.setProductVisibility(productId, visible);
        return ResponseEntity.ok().build();
    }

    // Reports
    @GetMapping("/reports")
    public ResponseEntity<List<ReportDTO>> getAllReports() {
        return ResponseEntity.ok(adminService.getAllReports());
    }

    @PutMapping("/reports/{reportId}/resolve")
    public ResponseEntity<Void> resolveReport(@PathVariable Long reportId) {
        // Simple resolution for now
        adminService.createReportResolution(reportId, "RESOLVED");
        return ResponseEntity.ok().build();
    }

    // Pending Seller Applications
    @GetMapping("/seller-applications/pending")
    public ResponseEntity<List<com.example.jhapcham.seller.SellerApplication>> getPendingApplications() {
        return ResponseEntity.ok(adminService.getPendingApplications());
    }

    @PutMapping("/seller-applications/{appId}/approve")
    public ResponseEntity<Void> approveApplication(@PathVariable Long appId) {
        adminService.approveSellerApplication(appId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/seller-applications/{appId}/reject")
    public ResponseEntity<Void> rejectApplication(@PathVariable Long appId) {
        adminService.rejectSellerApplication(appId);
        return ResponseEntity.ok().build();
    }
}
