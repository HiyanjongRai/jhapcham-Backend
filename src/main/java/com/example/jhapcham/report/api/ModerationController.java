package com.example.jhapcham.report.api;

import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.report.application.ReportService;
import com.example.jhapcham.report.domain.ReportStatus;
import com.example.jhapcham.security.CurrentUserService;
import com.example.jhapcham.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class ModerationController {

    private final ReportService reportService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<?> getReports(
            @RequestParam String type,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        try {
            User admin = currentUserService.requireUser(authentication);
            currentUserService.requireAdmin(admin);

            ReportStatus reportStatus = null;
            if (status != null && !status.isEmpty()) {
                reportStatus = ReportStatus.valueOf(status.toUpperCase());
            }

            if ("PRODUCT".equalsIgnoreCase(type)) {
                return ResponseEntity.ok(reportService.getProductReports(reportStatus));
            } else if ("SELLER".equalsIgnoreCase(type)) {
                return ResponseEntity.ok(reportService.getSellerReports(reportStatus));
            } else if ("CUSTOMER".equalsIgnoreCase(type)) {
                return ResponseEntity.ok(reportService.getCustomerReports(reportStatus));
            } else {
                throw new IllegalArgumentException("Invalid report type. Supported: PRODUCT, SELLER, CUSTOMER");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
