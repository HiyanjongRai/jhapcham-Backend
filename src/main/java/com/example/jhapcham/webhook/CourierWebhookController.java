package com.example.jhapcham.webhook;

import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.delivery.DeliveryStatus;
import com.example.jhapcham.delivery.Shipment;
import com.example.jhapcham.delivery.TrackingService;
import com.example.jhapcham.delivery.ShipmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;

/**
 * Receives real-time delivery status updates pushed by the Courier service.
 *
 * SECURITY: All incoming webhooks MUST carry a valid X-Hub-Signature header.
 * The signature is HMAC-SHA256(request body, WEBHOOK_SECRET).
 * If the signature does not match, the request is rejected with 401.
 *
 * IDEMPOTENCY: If the order is already in the target state, the webhook is
 * acknowledged (200 OK) but no state change is applied, preventing duplicate processing.
 */
@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class CourierWebhookController {

    private final TrackingService trackingService;
    private final ShipmentRepository shipmentRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.webhook.courier.secret}")
    private String webhookSecret;

    // Terminal states that must not be overwritten by any incoming webhook
    private static final Set<DeliveryStatus> TERMINAL_STATES = Set.of(
            DeliveryStatus.DELIVERED,
            DeliveryStatus.RETURN_TO_SELLER,
            DeliveryStatus.CANCELLED
    );

    @PostMapping("/courier-status")
    public ResponseEntity<?> receiveCourierStatusUpdate(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Hub-Signature", required = false) String signature) {

        // 1. HMAC Signature verification
        if (!isValidSignature(rawBody, signature)) {
            log.warn("Webhook rejected: invalid or missing X-Hub-Signature");
            return ResponseEntity.status(401).body(new ErrorResponse("Invalid webhook signature."));
        }

        CourierStatusWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, CourierStatusWebhookPayload.class);
        } catch (Exception exception) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid webhook payload."));
        }

        // 2. Resolve the shipment by tracking ID
        Shipment shipment = shipmentRepository.findByTrackingId(payload.getTrackingId()).orElse(null);
        if (shipment == null) {
            log.warn("Webhook ignored: unknown trackingId={}", payload.getTrackingId());
            // Return 200 to prevent the courier system from endlessly retrying for unknown IDs
            return ResponseEntity.ok("Tracking ID not found. Acknowledged.");
        }

        // 3. Idempotency guard: ignore if already in terminal state
        if (TERMINAL_STATES.contains(shipment.getStatus())) {
            log.info("Webhook ignored: shipment {} already in terminal state {}", payload.getTrackingId(), shipment.getStatus());
            return ResponseEntity.ok("Shipment already in final state. Acknowledged.");
        }

        // 4. Parse the incoming delivery status
        DeliveryStatus incomingStatus;
        try {
            incomingStatus = DeliveryStatus.valueOf(payload.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Unknown delivery status: " + payload.getStatus()));
        }

        // 5. Delegate to TrackingService — this handles OTP (for DELIVERED),
        //    COD collection, return logistics, and order status sync
        try {
            trackingService.updateStatus(
                    payload.getTrackingId(),
                    incomingStatus,
                    payload.getLocation(),
                    payload.getNote() != null ? payload.getNote() : "Webhook update",
                    payload.getVerificationOtp()
            );
            log.info("Webhook processed: trackingId={} → status={}", payload.getTrackingId(), incomingStatus);
            return ResponseEntity.ok("Status updated successfully.");
        } catch (RuntimeException e) {
            log.error("Webhook processing error for {}: {}", payload.getTrackingId(), e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Verify the HMAC-SHA256 signature from the Courier service.
     * Expected header: X-Hub-Signature: sha256=<hex_digest>
     */
    private boolean isValidSignature(String rawBody, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        if (webhookSecret == null || webhookSecret.isBlank() || webhookSecret.startsWith("change-me-in-production")) {
            log.error("Courier webhook secret is not configured with a strong non-default value");
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = "sha256=" + HexFormat.of().formatHex(rawHmac);
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    // ============================================================
    // PAYLOAD DTO
    // ============================================================
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourierStatusWebhookPayload {
        private String trackingId;
        private String status;
        private String location;
        private String note;
        private LocalDateTime timestamp;
        private String verificationOtp; // Required only for DELIVERED
    }
}
