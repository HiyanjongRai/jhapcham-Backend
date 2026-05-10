package com.example.jhapcham.seller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellerApplicationController {

    private final SellerApplicationService applicationService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @PostMapping(value = "/application", consumes = {"multipart/form-data"})
    public ResponseEntity<?> submitApplication(
            @RequestParam Long userId,
            @RequestParam String storeName,
            @RequestParam String address,
            @RequestParam(required = false) MultipartFile idDocument,
            @RequestParam(required = false) MultipartFile businessLicense,
            @RequestParam(required = false) MultipartFile taxCertificate,
            Authentication authentication
    ) {
        currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);

        SellerApplication app = applicationService.submitApplication(
                userId,
                storeName,
                address,
                idDocument,
                businessLicense,
                taxCertificate
        );

        return ResponseEntity.ok(
                Map.of(
                        "message", "Application submitted. Waiting for admin approval",
                        "applicationId", app.getId(),
                        "applicationStatus", app.getStatus().name(),
                        "userStatus", app.getUser().getStatus().name()
                )
        );
    }


}
