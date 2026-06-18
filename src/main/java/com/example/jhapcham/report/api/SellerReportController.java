package com.example.jhapcham.report.api;

import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.report.application.ReportService;
import com.example.jhapcham.report.dto.ReportResolutionDTO;
import com.example.jhapcham.report.dto.SellerReportRequestDTO;
import com.example.jhapcham.security.CurrentUserService;
import com.example.jhapcham.security.RequestRateLimiter;
import com.example.jhapcham.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports/seller")
@RequiredArgsConstructor
public class SellerReportController {

    private final ReportService reportService;
    private final CurrentUserService currentUserService;
    private final RequestRateLimiter rateLimiter;

    @PostMapping
    public ResponseEntity<?> reportSeller(@RequestBody SellerReportRequestDTO dto, Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            rateLimiter.check("report:seller:user:" + user.getId(), 10, 3600);
            return ResponseEntity.ok(reportService.createSellerReport(user, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/ref/{publicReferenceId}")
    public ResponseEntity<?> getSellerReportByReference(
            @PathVariable String publicReferenceId,
            Authentication authentication) {
        try {
            User admin = currentUserService.requireUser(authentication);
            currentUserService.requireAdmin(admin);
            return ResponseEntity.ok(reportService.getSellerReportByReference(publicReferenceId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> resolveSellerReport(
            @PathVariable Long id,
            @RequestBody ReportResolutionDTO dto,
            Authentication authentication) {
        try {
            User admin = currentUserService.requireUser(authentication);
            currentUserService.requireAdmin(admin);
            return ResponseEntity.ok(reportService.resolveSellerReport(admin, id, dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
