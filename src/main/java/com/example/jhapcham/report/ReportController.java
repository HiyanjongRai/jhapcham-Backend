package com.example.jhapcham.report;

import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.report.dto.*;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import com.example.jhapcham.common.FileStorageService;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<?> createReport(
            @ModelAttribute ReportRequestDTO request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getAuthenticatedUser(userDetails);
            
            if (files != null && !files.isEmpty()) {
                List<String> evidenceUrls = new ArrayList<>();
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        String path = fileStorageService.save(file, "reports", file.getOriginalFilename());
                        evidenceUrls.add(path);
                    }
                }
                request.setEvidenceUrls(evidenceUrls);
            }
            
            return ResponseEntity.ok(reportService.createReport(user.getId(), request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyReports(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getAuthenticatedUser(userDetails);
            return ResponseEntity.ok(reportService.getMyReports(user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getSellerReports(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getAuthenticatedUser(userDetails);
            return ResponseEntity.ok(reportService.getSellerReports(user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/seller-action")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> sellerAction(
            @PathVariable Long id,
            @RequestBody SellerActionDTO action,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getAuthenticatedUser(userDetails);
            return ResponseEntity.ok(reportService.sellerAction(id, user.getId(), action));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/disputes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDisputes() {
        try {
            return ResponseEntity.ok(reportService.getDisputedReports());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{id}/admin-action")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminAction(
            @PathVariable Long id,
            @RequestBody AdminActionDTO action) {
        try {
            return ResponseEntity.ok(reportService.adminAction(id, action));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReport(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(reportService.getReport(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) throw new RuntimeException("Unauthorized");
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
