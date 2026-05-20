package com.example.jhapcham.refund;

import com.example.jhapcham.security.CurrentUserService;
import com.example.jhapcham.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final CurrentUserService currentUserService;

    @PostMapping(value = "/api/refunds", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RefundResponseDTO> createRefundJson(
            @Valid @RequestBody RefundCreateRequestDTO request,
            Authentication authentication) {
        User actor = currentUserService.requireUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(refundService.createRefund(actor, request, List.of()));
    }

    @PostMapping(value = "/api/refunds", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RefundResponseDTO> createRefundMultipart(
            @Valid @RequestPart("request") RefundCreateRequestDTO request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) {
        User actor = currentUserService.requireUser(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(refundService.createRefund(actor, request, files));
    }

    @GetMapping("/api/refunds/my")
    public ResponseEntity<?> getMyRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        User actor = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.getMyRefunds(actor, pageable(page, size)));
    }

    @GetMapping("/api/refunds/{id}")
    public ResponseEntity<RefundResponseDTO> getRefund(@PathVariable Long id, Authentication authentication) {
        User actor = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(refundService.getRefund(id, actor));
    }

    @PostMapping("/api/refunds/{id}/cancel")
    public ResponseEntity<RefundResponseDTO> cancelRefund(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        User actor = currentUserService.requireUser(authentication);
        String note = body != null ? body.get("note") : null;
        return ResponseEntity.ok(refundService.cancelRefund(id, actor, note));
    }

    @GetMapping("/api/seller/refunds")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getSellerRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        User actor = currentUserService.requireUser(authentication);
        currentUserService.requireSellerSelfOrAdmin(actor, actor.getId());
        return ResponseEntity.ok(refundService.getSellerRefunds(actor, pageable(page, size)));
    }

    @PostMapping(value = "/api/seller/refunds/{id}/review", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<RefundResponseDTO> sellerReviewJson(
            @PathVariable Long id,
            @Valid @RequestBody RefundReviewRequestDTO request,
            Authentication authentication) {
        User actor = currentUserService.requireUser(authentication);
        currentUserService.requireSellerSelfOrAdmin(actor, actor.getId());
        return ResponseEntity.ok(refundService.sellerReview(id, actor, request, List.of()));
    }

    @PostMapping(value = "/api/seller/refunds/{id}/review", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<RefundResponseDTO> sellerReviewMultipart(
            @PathVariable Long id,
            @Valid @RequestPart("review") RefundReviewRequestDTO request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) {
        User actor = currentUserService.requireUser(authentication);
        currentUserService.requireSellerSelfOrAdmin(actor, actor.getId());
        return ResponseEntity.ok(refundService.sellerReview(id, actor, request, files));
    }

    @GetMapping("/api/admin/refunds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAdminRefunds(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(refundService.getAdminRefunds(status, pageable(page, size)));
    }

    @PostMapping("/api/admin/refunds/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefundResponseDTO> adminApprove(
            @PathVariable Long id,
            @Valid @RequestBody RefundReviewRequestDTO request,
            Authentication authentication) {
        User actor = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(actor);
        return ResponseEntity.ok(refundService.adminApprove(id, actor, request));
    }

    @PostMapping("/api/admin/refunds/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefundResponseDTO> retryRefund(@PathVariable Long id, Authentication authentication) {
        User actor = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(actor);
        return ResponseEntity.ok(refundService.retryGatewayRefund(id, actor));
    }

    @PostMapping("/api/admin/refunds/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefundResponseDTO> confirmManualOrGateway(
            @Valid @RequestBody RefundGatewayConfirmationDTO confirmation,
            Authentication authentication) {
        User actor = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(actor);
        return ResponseEntity.ok(refundService.confirmGatewayRefund(confirmation, actor));
    }

    @GetMapping("/api/admin/refunds/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefundAnalyticsDTO> refundAnalytics(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(refundService.getAnalytics(days));
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
