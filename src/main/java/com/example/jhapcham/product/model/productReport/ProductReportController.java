package com.example.jhapcham.product.model.productReport;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProductReportController {

    private final ProductReportService service;

    // Submit a report (authenticated user)
    @PostMapping("/api/products/{productId}/reports")
    public ResponseEntity<?> submit(
            @PathVariable Long productId,
            @RequestParam Long userId,
            @RequestParam String reason
    ) {
        try {
            ProductReport r = service.submit(productId, userId, reason);
            return ResponseEntity.ok(Map.of(
                    "message", "Report submitted",
                    "reportId", r.getId(),
                    "status", r.getStatus().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Admin: list reports for a product
    @GetMapping("/api/products/{productId}/reports")
    public ResponseEntity<?> listForProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(service.listByProduct(productId));
    }

    // Admin: list all reports (optionally by status)
    @GetMapping("/api/reports")
    public ResponseEntity<?> listAll(@RequestParam(required = false) String status) {
        ProductReport.Status s = null;
        if (status != null) {
            try { s = ProductReport.Status.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(service.listByStatus(s));
    }

    // Admin: update report status / note
    @PatchMapping("/api/reports/{reportId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long reportId,
            @RequestParam String status,
            @RequestParam(required = false) String adminNote
    ) {
        try {
            ProductReport.Status s = ProductReport.Status.valueOf(status.toUpperCase());
            ProductReport updated = service.updateStatus(reportId, s, adminNote);
            return ResponseEntity.ok(Map.of(
                    "message", "Report status updated",
                    "reportId", updated.getId(),
                    "status", updated.getStatus().name()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
