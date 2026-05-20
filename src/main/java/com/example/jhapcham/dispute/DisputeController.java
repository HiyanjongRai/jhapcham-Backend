package com.example.jhapcham.dispute;

import com.example.jhapcham.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @PostMapping("/initiate")
    public ResponseEntity<?> initiateDispute(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);

            Long orderId = Long.parseLong(body.get("orderId").toString());
            String title = (String) body.get("title");
            String description = (String) body.get("description");

            DisputeResponseDTO response = disputeService.initiateDispute(user.getId(), orderId, title, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error initiating dispute: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{disputeId}/upload-evidence")
    public ResponseEntity<?> uploadEvidence(
            @PathVariable Long disputeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);

            disputeService.uploadEvidence(disputeId, user.getId(), file, description);
            return ResponseEntity.ok(Map.of("message", "Evidence uploaded successfully"));
        } catch (Exception e) {
            log.error("Error uploading evidence: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-disputes")
    public ResponseEntity<?> getMyDisputes(Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);

            List<DisputeResponseDTO> disputes = disputeService.getUserDisputes(user.getId());
            return ResponseEntity.ok(disputes);
        } catch (Exception e) {
            log.error("Error fetching disputes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{disputeId}")
    public ResponseEntity<?> getDispute(@PathVariable Long disputeId, Authentication authentication) {
        try {
            DisputeResponseDTO dispute = disputeService.getDispute(disputeId, currentUserService.requireUser(authentication));
            return ResponseEntity.ok(dispute);
        } catch (Exception e) {
            log.error("Error fetching dispute: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── GAP 2: STATUS TRANSITION ENDPOINTS ─────────────────────────────────────

    @PostMapping("/{disputeId}/escalate")
    public ResponseEntity<?> escalate(@PathVariable Long disputeId, Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            return ResponseEntity.ok(disputeService.escalateDispute(disputeId, user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{disputeId}/seller-respond")
    public ResponseEntity<?> sellerRespond(@PathVariable Long disputeId, Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            return ResponseEntity.ok(disputeService.sellerRespond(disputeId, user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{disputeId}/seller-approve")
    public ResponseEntity<?> sellerApprove(@PathVariable Long disputeId, Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            return ResponseEntity.ok(disputeService.sellerApprove(disputeId, user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{disputeId}/seller-decline")
    public ResponseEntity<?> sellerDecline(@PathVariable Long disputeId, Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            return ResponseEntity.ok(disputeService.sellerDecline(disputeId, user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{disputeId}/customer-respond")
    public ResponseEntity<?> customerRespond(@PathVariable Long disputeId, Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            return ResponseEntity.ok(disputeService.customerRespond(disputeId, user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Admin endpoints
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingDisputes() {
        try {
            List<DisputeResponseDTO> disputes = disputeService.getPendingDisputes();
            return ResponseEntity.ok(disputes);
        } catch (Exception e) {
            log.error("Error fetching pending disputes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/admin/{disputeId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resolveDispute(
            @PathVariable Long disputeId,
            @RequestBody Map<String, String> body) {
        try {
            String resolution = body.get("resolution");
            String adminNotes = body.getOrDefault("adminNotes", "");

            DisputeResponseDTO response = disputeService.resolveDispute(disputeId, resolution, adminNotes);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resolving dispute: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
