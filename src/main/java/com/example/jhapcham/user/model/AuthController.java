package com.example.jhapcham.user.model;

import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.seller.PendingSellerApplicationDTO;
import com.example.jhapcham.seller.SellerApplication;
import com.example.jhapcham.seller.SellerApplicationService;
import com.example.jhapcham.seller.SellerApplicationStatusDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SellerApplicationService sellerApplicationService;
    private final UserRepository userRepository;
    private final com.example.jhapcham.security.SimpleTokenService tokenService;

    @PostMapping("/register/customer")
    public ResponseEntity<AuthResponseDTO> registerCustomer(@Valid @RequestBody RegisterRequestDTO req) {
        User user = authService.registerCustomer(req);
        String token = tokenService.generateToken(user.getUsername(), user.getRole().name());
        AuthResponseDTO response = new AuthResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                "Customer created successfully",
                token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/seller")
    public ResponseEntity<AuthResponseDTO> registerSeller(@Valid @RequestBody RegisterRequestDTO req) {
        User user = authService.registerSeller(req);
        String token = tokenService.generateToken(user.getUsername(), user.getRole().name());
        AuthResponseDTO response = new AuthResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                "Seller registered. Please submit application documents",
                token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/admin")
    public ResponseEntity<AuthResponseDTO> registerAdmin(@Valid @RequestBody RegisterRequestDTO req) {
        User user = authService.registerAdmin(req);
        String token = tokenService.generateToken(user.getUsername(), user.getRole().name());
        AuthResponseDTO response = new AuthResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                "Admin created successfully",
                token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO req) {
        try {
            User user = authService.login(req);
            String token = tokenService.generateToken(user.getUsername(), user.getRole().name());
            AuthResponseDTO response = new AuthResponseDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole(),
                    user.getStatus(),
                    "Login successful",
                    token);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(new ErrorResponse(e.getMessage()));
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
            @ModelAttribute UserProfileUpdateRequestDTO dto) {
        try {
            User updated = authService.updateUserById(userId, dto);
            UserProfileResponseDTO response = UserProfileResponseDTO.builder()
                    .id(updated.getId())
                    .username(updated.getUsername())
                    .fullName(updated.getFullName())
                    .email(updated.getEmail())
                    .contactNumber(updated.getContactNumber())
                    .profileImagePath(updated.getProfileImagePath())
                    .role(updated.getRole())
                    .status(updated.getStatus())
                    .build();
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
    public ResponseEntity<?> listSellersAndCustomersForAdmin(@PathVariable Long adminId) {
        try {
            User admin = assertAdmin(adminId);

            List<UserProfileResponseDTO> users = userRepository.findAll()
                    .stream()
                    .filter(u -> u.getRole() == Role.CUSTOMER || u.getRole() == Role.SELLER)
                    .map(u -> UserProfileResponseDTO.builder()
                            .id(u.getId())
                            .username(u.getUsername())
                            .fullName(u.getFullName())
                            .email(u.getEmail())
                            .contactNumber(u.getContactNumber())
                            .profileImagePath(u.getProfileImagePath())
                            .role(u.getRole())
                            .status(u.getStatus())
                            .build())
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
            @RequestBody Map<String, String> body) {
        try {
            User admin = assertAdmin(adminId);

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

            UserProfileResponseDTO dto = UserProfileResponseDTO.builder()
                    .id(updated.getId())
                    .username(updated.getUsername())
                    .fullName(updated.getFullName())
                    .email(updated.getEmail())
                    .contactNumber(updated.getContactNumber())
                    .profileImagePath(updated.getProfileImagePath())
                    .role(updated.getRole())
                    .status(updated.getStatus())
                    .build();

            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to change status"));
        }
    }

    // --------------- HELPER ---------------

    private User assertAdmin(Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("User is not admin");
        }
        return admin;
    }

    @GetMapping("/seller/{userId}/application-status")
    public ResponseEntity<SellerApplicationStatusDTO> getSellerApplicationStatus(@PathVariable Long userId) {

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
}
