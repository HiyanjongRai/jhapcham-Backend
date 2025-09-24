package com.example.jhapcham.admin.repository.controller;

import com.example.jhapcham.seller.Service.SellerApplicationService;
import com.example.jhapcham.seller.model.SellerApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sellers")
@RequiredArgsConstructor
public class AdminSellerReviewController {

    private final SellerApplicationService applicationService;

    // List pending applications
    @GetMapping("/applications/pending")
    public ResponseEntity<List<SellerApplication>> listPending() {
        return ResponseEntity.ok(applicationService.listPending());
    }

    // Get one application (admin can view doc paths)
    @GetMapping("/applications/{appId}")
    public ResponseEntity<SellerApplication> getApplication(@PathVariable Long appId) {
        return ResponseEntity.ok(applicationService.getApplication(appId));
    }

    // Approve application
    @PostMapping("/applications/{appId}/approve")
    public ResponseEntity<?> approve(@PathVariable Long appId, @RequestParam(required = false) String note) {
        SellerApplication app = applicationService.approve(appId, note);
        return ResponseEntity.ok(Map.of(
                "message", "Seller approved and profile created.",
                "applicationId", app.getId(),
                "status", app.getStatus().name()
        ));
    }

    // Reject application
    @PostMapping("/applications/{appId}/reject")
    public ResponseEntity<?> reject(@PathVariable Long appId, @RequestParam(required = false) String note) {
        SellerApplication app = applicationService.reject(appId, note);
        return ResponseEntity.ok(Map.of(
                "message", "Seller application rejected.",
                "applicationId", app.getId(),
                "status", app.getStatus().name(),
                "note", app.getReviewNote()
        ));
    }
}
