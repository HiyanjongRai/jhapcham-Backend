package com.example.jhapcham.report.api;

import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.report.application.ReportService;
import com.example.jhapcham.report.dto.CustomerReportRequestDTO;
import com.example.jhapcham.report.dto.ReportResolutionDTO;
import com.example.jhapcham.security.CurrentUserService;
import com.example.jhapcham.security.RequestRateLimiter;
import com.example.jhapcham.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports/customer")
@RequiredArgsConstructor
public class CustomerReportController {

    private final ReportService reportService;
    private final CurrentUserService currentUserService;
    private final RequestRateLimiter rateLimiter;

    @PostMapping
    public ResponseEntity<?> reportCustomer(@RequestBody CustomerReportRequestDTO dto, Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            rateLimiter.check("report:customer:user:" + user.getId(), 10, 3600);
            return ResponseEntity.ok(reportService.createCustomerReport(user, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/ref/{publicReferenceId}")
    public ResponseEntity<?> getCustomerReportByReference(
            @PathVariable String publicReferenceId,
            Authentication authentication) {
        try {
            User admin = currentUserService.requireUser(authentication);
            currentUserService.requireAdmin(admin);
            return ResponseEntity.ok(reportService.getCustomerReportByReference(publicReferenceId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> resolveCustomerReport(
            @PathVariable Long id,
            @RequestBody ReportResolutionDTO dto,
            Authentication authentication) {
        try {
            User admin = currentUserService.requireUser(authentication);
            currentUserService.requireAdmin(admin);
            return ResponseEntity.ok(reportService.resolveCustomerReport(admin, id, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
