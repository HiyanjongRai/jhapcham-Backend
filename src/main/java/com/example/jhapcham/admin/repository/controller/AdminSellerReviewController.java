package com.example.jhapcham.admin.repository.controller;

import com.example.jhapcham.seller.Service.SellerApplicationService;
import com.example.jhapcham.seller.model.SellerApplication;
import com.example.jhapcham.seller.repository.SellerApplicationRepository;
import com.example.jhapcham.seller.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sellers")
@RequiredArgsConstructor
public class AdminSellerReviewController {

    private final SellerProfileRepository sellerProfileRepository;
    private final SellerApplicationRepository sellerApplicationRepository;
    private final SellerApplicationService applicationService;

    // List pending applications
    @GetMapping("/applications/pending")
    public ResponseEntity<?> listPendingWithDocs() {
        List<SellerApplication> pendingApps = applicationService.listPending();

        var dtos = pendingApps.stream().map(app -> {
            String idDoc = app.getIdDocumentPath() != null ? "/api/admin/sellers/download?path=" + app.getIdDocumentPath() : null;
            String license = app.getBusinessLicensePath() != null ? "/api/admin/sellers/download?path=" + app.getBusinessLicensePath() : null;
            String tax = app.getTaxCertificatePath() != null ? "/api/admin/sellers/download?path=" + app.getTaxCertificatePath() : null;

            // Get seller logo if profile exists
            String logo = app.getUser().getId() != null ?
                    sellerProfileRepository.findByUser(app.getUser())
                            .map(profile -> profile.getLogoImagePath() != null ?
                                    "/api/admin/sellers/download?path=" + profile.getLogoImagePath() : null)
                            .orElse(null)
                    : null;

            return AdminSellerApplicationDTO.builder()
                    .applicationId(app.getId())
                    .userId(app.getUser().getId())
                    .username(app.getUser().getUsername())
                    .status(app.getStatus())
                    .storeName(app.getStoreName())
                    .address(app.getAddress())
                    .idDocumentUrl(idDoc)
                    .businessLicenseUrl(license)
                    .taxCertificateUrl(tax)
                    .logoImageUrl(logo)
                    .build();
        }).toList();

        return ResponseEntity.ok(dtos);
    }


    // Get one application (admin can view doc paths)
    @GetMapping("/applications/{appId}")
    public ResponseEntity<SellerApplication> getApplication(@PathVariable Long appId) {
        return ResponseEntity.ok(applicationService.getApplication(appId));
    }

    // Approve application
    @PostMapping("/applications/{appId}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long appId,
            @RequestBody Map<String, String> body
    ) {
        String note = body.get("note");
        SellerApplication app = applicationService.approve(appId, note);

        return ResponseEntity.ok(Map.of(
                "message", "Seller approved and profile created.",
                "applicationId", app.getId(),
                "status", app.getStatus().name(),
                "note", note
        ));
    }


    // Reject application
    @PostMapping("/applications/{appId}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long appId,
            @RequestBody Map<String, String> body
    ) {
        String note = body.get("note");
        SellerApplication app = applicationService.reject(appId, note);

        return ResponseEntity.ok(Map.of(
                "message", "Seller application rejected.",
                "applicationId", app.getId(),
                "status", app.getStatus().name(),
                "note", note
        ));
    }
}

