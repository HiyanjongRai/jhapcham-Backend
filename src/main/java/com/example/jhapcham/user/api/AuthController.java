package com.example.jhapcham.user.api;


import com.example.jhapcham.user.application.*;
import com.example.jhapcham.user.domain.*;
import com.example.jhapcham.user.dto.*;
import com.example.jhapcham.user.persistence.*;
import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.seller.dto.PendingSellerApplicationDTO;
import com.example.jhapcham.seller.domain.SellerApplication;
import com.example.jhapcham.seller.application.SellerApplicationService;
import com.example.jhapcham.seller.dto.SellerApplicationStatusDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SellerApplicationService sellerApplicationService;
    private final UserRepository userRepository;
    private final com.example.jhapcham.security.TokenService tokenService;
    private final com.example.jhapcham.security.RefreshTokenCookieService refreshTokenCookieService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;
    private final com.example.jhapcham.security.RequestRateLimiter rateLimiter;

    @PostMapping("/register/customer")
    public ResponseEntity<AuthResponseDTO> registerCustomer(@Valid @RequestBody RegisterRequestDTO req,
                                                            HttpServletResponse response) {
        User user = authService.registerCustomer(req);
        return buildAuthenticatedResponse(user, "Customer created successfully", response);
    }

    @PostMapping("/register/seller")
    public ResponseEntity<AuthResponseDTO> registerSeller(@Valid @RequestBody RegisterRequestDTO req,
                                                          HttpServletResponse response) {
        User user = authService.registerSeller(req);
        return buildAuthenticatedResponse(user, "Seller registered. Please submit application documents", response);
    }

    @PostMapping("/register/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponseDTO> registerAdmin(@Valid @RequestBody RegisterRequestDTO req,
                                                         HttpServletResponse response) {
        User user = authService.registerAdmin(req);
        return buildAuthenticatedResponse(user, "Admin created successfully", response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO req,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse response) {
        try {
            rateLimiter.check(rateKey(httpRequest, "auth:login"), 20, 60);
            User user = authService.login(req);
            return buildAuthenticatedResponse(user, "Login successful", response);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleAuth(@RequestBody Map<String, String> body,
                                        HttpServletResponse response) {
        try {
            String idTokenString = body.get("credential");
            if (idTokenString == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Google credential is required"));
            }

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList("983073986551-a49ce7tnjh29fccqnqp1v92ma4i3b3ba.apps.googleusercontent.com"))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String userId = payload.getSubject();
                String email = payload.getEmail();
                String name = (String) payload.get("name");

                Role requestedRole = null;
                String roleHint = body.get("role");
                if (roleHint == null) roleHint = body.get("requestedRole");
                if (roleHint == null) roleHint = body.get("accountType");
                if (roleHint == null && "seller".equalsIgnoreCase(body.get("intent"))) {
                    roleHint = "SELLER";
                }
                if (roleHint != null) {
                    try {
                        requestedRole = Role.valueOf(roleHint.toUpperCase());
                    } catch (Exception e) { /* fallback to default */ }
                }

                User user = authService.loginWithGoogle(email, name, userId, requestedRole);
                return buildAuthenticatedResponse(user, "Google Auth successful", response);
            } else {
                return ResponseEntity.status(401).body(new ErrorResponse("Invalid Google token"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body(new ErrorResponse("Google Authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/upgrade-to-seller")
    public ResponseEntity<?> upgradeToSeller(Authentication authentication,
                                            HttpServletResponse response) {
        try {
            User currentUser = currentUserService.requireUser(authentication);
            User seller = authService.upgradeCustomerToSeller(currentUser.getId());
            return buildAuthenticatedResponse(seller, "Seller account started. Please submit application documents", response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            refreshTokenCookieService.ensureTrustedOrigin(request);
            String rawRefreshToken = refreshTokenCookieService.extractRefreshToken(request);
            var rotated = tokenService.rotateRefreshToken(rawRefreshToken);
            response.addHeader(HttpHeaders.SET_COOKIE,
                    refreshTokenCookieService.createRefreshTokenCookie(rotated.rawRefreshToken()));
            return ResponseEntity.ok(rotated.authResponse());
        } catch (RuntimeException e) {
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService.createClearedRefreshTokenCookie());
            return ResponseEntity.status(401).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            refreshTokenCookieService.ensureTrustedOrigin(request);
            String rawRefreshToken = refreshTokenCookieService.extractRefreshToken(request);
            tokenService.revokeRefreshToken(rawRefreshToken);
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService.createClearedRefreshTokenCookie());
            return ResponseEntity.ok(Map.of("message", "Logout successful"));
        } catch (RuntimeException e) {
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService.createClearedRefreshTokenCookie());
            return ResponseEntity.status(401).body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponseDTO> getCurrentUser(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);

        UserProfileResponseDTO response = authService.convertToProfileDto(user);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        try {
            rateLimiter.check(rateKey(httpRequest, "auth:forgot-password"), 5, 900);
            String email = body.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Email is required"));
            }
            authService.generatePasswordResetOtp(email);
            return ResponseEntity.ok(Map.of("message", "OTP sent to your email"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        } catch (RuntimeException e) {
             return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<?> verifyResetOtp(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        try {
            rateLimiter.check(rateKey(httpRequest, "auth:verify-reset-otp"), 10, 900);
            String email = body.get("email");
            String otp = body.get("otp");
            if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Email and OTP are required"));
            }
            authService.verifyPasswordResetOtp(email, otp);
            return ResponseEntity.ok(Map.of("message", "OTP verified. You may proceed to reset password."));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        } catch (RuntimeException e) {
             return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        try {
            rateLimiter.check(rateKey(httpRequest, "auth:reset-password"), 5, 900);
            String email = body.get("email");
            String otp = body.get("otp");
            String newPassword = body.get("newPassword");
            
            if (email == null || email.isBlank() || otp == null || otp.isBlank() || newPassword == null || newPassword.isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Email, OTP, and newPassword are required"));
            }
            authService.resetPassword(email, otp, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now login."));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(new ErrorResponse(e.getReason()));
        } catch (RuntimeException e) {
             return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // --------------- ADMIN ONLY: SELLER APPLICATIONS ---------------

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/seller-applications/pending")
    public ResponseEntity<List<PendingSellerApplicationDTO>> listPendingApplications() {
        List<SellerApplication> apps = sellerApplicationService.listPending();

        List<PendingSellerApplicationDTO> dtos = apps.stream()
                .map(app -> new PendingSellerApplicationDTO(
                        app.getId(),
                        app.getUser().getId(),
                        app.getUser().getUsername(),
                        app.getUser().getEmail(),
                        app.getStoreName(),
                        app.getAddress(),
                        app.getStatus().name(),
                        app.getSubmittedAt(),
                        app.getIdDocumentPath(),
                        app.getBusinessLicensePath(),
                        app.getTaxCertificatePath()))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/seller-applications/{appId}/approve")
    public ResponseEntity<Map<String, Object>> approveSellerApplication(
            @PathVariable Long appId,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;

        SellerApplication app = sellerApplicationService.approve(appId, note);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Seller approved and profile created");
        response.put("applicationId", app.getId());
        response.put("status", app.getStatus().name());
        if (note != null) {
            response.put("note", note);
        }

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/seller-applications/{appId}/reject")
    public ResponseEntity<Map<String, Object>> rejectSellerApplication(
            @PathVariable Long appId,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;

        SellerApplication app = sellerApplicationService.reject(appId, note);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "Seller application rejected");
        response.put("applicationId", app.getId());
        response.put("status", app.getStatus().name());
        if (note != null) {
            response.put("note", note);
        }

        return ResponseEntity.ok(response);
    }

    // --------------- USER PROFILE UPDATE (SELF) ---------------

    @PutMapping("/profile/{userId}")
    public ResponseEntity<?> updateProfileById(
            @PathVariable Long userId,
            @ModelAttribute UserProfileUpdateRequestDTO dto,
            Authentication authentication) {
        try {
            User actor = currentUserService.requireUser(authentication);
            currentUserService.requireSelfOrAdmin(actor, userId);
            User updated = authService.updateUserById(userId, dto);
            UserProfileResponseDTO response = authService.convertToProfileDto(updated);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to update profile"));
        }
    }

    // --------------- ADMIN WITH ADMIN ID: VIEW ALL USERS ---------------

    @GetMapping("/admin/{adminId}/users")
    public ResponseEntity<?> listSellersAndCustomersForAdmin(@PathVariable Long adminId, Authentication authentication) {
        try {
            User admin = currentUserService.requireUser(authentication);
            currentUserService.requireAdmin(admin);

            List<UserProfileResponseDTO> users = userRepository.findAll()
                    .stream()
                    .filter(u -> u.getRole() == Role.CUSTOMER || u.getRole() == Role.SELLER)
                    .map(authService::convertToProfileDto)
                    .sorted((a, b) -> {
                        String n1 = a.getFullName();
                        String n2 = b.getFullName();
                        if (n1 == null && n2 == null)
                            return 0;
                        if (n1 == null)
                            return 1;
                        if (n2 == null)
                            return -1;
                        return n1.compareToIgnoreCase(n2);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(users);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        }
    }

    // ADMIN WITH ADMIN ID: change status of CUSTOMER or SELLER

    @PutMapping("/admin/{adminId}/users/{userId}/status")
    public ResponseEntity<?> changeUserStatus(
            @PathVariable Long adminId,
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        try {
            User admin = currentUserService.requireUser(authentication);
            currentUserService.requireAdmin(admin);

            User target = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (target.getRole() != Role.CUSTOMER && target.getRole() != Role.SELLER) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Only customer or seller status can be changed"));
            }

            String statusStr = body != null ? body.get("status") : null;
            if (statusStr == null || statusStr.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("status is required"));
            }

            Status newStatus;
            try {
                newStatus = Status.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid status value"));
            }

            User updated = authService.updateUserStatus(userId, newStatus);

            UserProfileResponseDTO dto = authService.convertToProfileDto(updated);

            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to change status"));
        }
    }

    // --------------- HELPER ---------------

    @GetMapping("/seller/{userId}/application-status")
    public ResponseEntity<SellerApplicationStatusDTO> getSellerApplicationStatus(@PathVariable Long userId,
            Authentication authentication) {
        User actor = currentUserService.requireUser(authentication);
        currentUserService.requireSelfOrAdmin(actor, userId);

        SellerApplication app = sellerApplicationService.findByUserId(userId);

        if (app == null) {
            SellerApplicationStatusDTO dto = new SellerApplicationStatusDTO(
                    false,
                    "NONE",
                    "No application submitted",
                    null,
                    null,
                    null);
            return ResponseEntity.ok(dto);
        }

        String msg;
        switch (app.getStatus()) {
            case PENDING:
                msg = "Your application is under review";
                break;
            case APPROVED:
                msg = "Your application is approved";
                break;
            case REJECTED:
                msg = "Your application was rejected";
                break;
            default:
                msg = "Unknown status";
        }

        SellerApplicationStatusDTO dto = new SellerApplicationStatusDTO(
                true,
                app.getStatus().name(),
                msg,
                app.getReviewNote(),
                app.getId(),
                app.getSubmittedAt());

        return ResponseEntity.ok(dto);

    }

    private static String rateKey(HttpServletRequest request, String action) {
        if (request == null) {
            return action;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            // Only use the first IP if multiple are present.
            int comma = ip.indexOf(',');
            ip = (comma > 0 ? ip.substring(0, comma) : ip).trim();
        } else {
            ip = request.getRemoteAddr();
        }

        if (ip == null || ip.isBlank()) {
            ip = "unknown";
        }
        return action + ":" + ip;
    }

    private ResponseEntity<AuthResponseDTO> buildAuthenticatedResponse(User user,
                                                                       String message,
                                                                       HttpServletResponse response) {
        var issuedTokens = tokenService.issueAuthResponse(user, message);
        response.addHeader(HttpHeaders.SET_COOKIE,
                refreshTokenCookieService.createRefreshTokenCookie(issuedTokens.rawRefreshToken()));
        return ResponseEntity.ok(issuedTokens.authResponse());
    }
}
