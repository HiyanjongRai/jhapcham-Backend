package com.example.jhapcham.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/payment/esewa")
@RequiredArgsConstructor
public class EsewaPaymentController {

    private final OrderRepository orderRepository;
    private final OrderStockService orderStockService;

    @Value("${esewa.secret.key}")
    private String esewaSecretKey;

    @Value("${esewa.product.code}")
    private String esewaProductCode;

    @Value("${esewa.api.url}")
    private String esewaApiUrl;

    @Value("${esewa.success.url}")
    private String successUrl;

    @Value("${esewa.failure.url}")
    private String failureUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Step 1: Initiate eSewa payment for an existing order.
     * Generates the HMAC-SHA256 signature and returns the form parameters.
     * POST /api/payment/esewa/initiate
     */
    @Transactional
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getPaymentMethod() != PaymentMethod.ESEWA) {
            return ResponseEntity.badRequest().body(Map.of("error", "Order is not set to eSewa payment"));
        }

        // Amount must be a string without decimals for simple integers
        String totalAmount = String.valueOf(order.getGrandTotal().longValue());
        
        // Generate a unique transaction UUID (orderId + timestamp)
        String transactionUuid = order.getId() + "_" + System.currentTimeMillis();

        // 1. Generate Signature
        // Format: total_amount=100,transaction_uuid=123_169,product_code=EPAYTEST
        String message = String.format("total_amount=%s,transaction_uuid=%s,product_code=%s",
                totalAmount, transactionUuid, esewaProductCode);
        
        String signature;
        try {
            signature = generateHmacSha256(message, esewaSecretKey);
        } catch (Exception e) {
            log.error("Failed to generate eSewa signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate payment signature"));
        }

        // 2. Save UUID for later verification
        order.setEsewaTransactionUuid(transactionUuid);
        orderRepository.save(order);

        // 3. Return the exact payload the frontend form needs
        Map<String, String> payload = new HashMap<>();
        payload.put("amount", totalAmount);
        payload.put("tax_amount", "0");
        payload.put("total_amount", totalAmount);
        payload.put("transaction_uuid", transactionUuid);
        payload.put("product_code", esewaProductCode);
        payload.put("product_service_charge", "0");
        payload.put("product_delivery_charge", "0");
        payload.put("success_url", successUrl);
        payload.put("failure_url", failureUrl);
        payload.put("signed_field_names", "total_amount,transaction_uuid,product_code");
        payload.put("signature", signature);
        
        // Include the URL so the frontend knows where to submit the form
        payload.put("epayUrl", esewaApiUrl);

        log.info("eSewa payment initiated for order {}. UUID: {}", orderId, transactionUuid);
        return ResponseEntity.ok(payload);
    }

    /**
     * Step 2: Verify eSewa payment after user returns with base64 encoded data
     * POST /api/payment/esewa/verify
     */
    @Transactional
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> body) {
        String base64Data = (String) body.get("data");
        
        if (base64Data == null || base64Data.isEmpty()) {
             return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No data provided"));
        }

        try {
            // Decode Base64
            String decodedStr = new String(Base64.getDecoder().decode(base64Data), StandardCharsets.UTF_8);
            
            // Parse JSON
            Map<String, Object> data = objectMapper.readValue(decodedStr, Map.class);
            
            String transactionCode = (String) data.get("transaction_code");
            String status = (String) data.get("status");
            String totalAmount = (String) data.get("total_amount");
            String transactionUuid = (String) data.get("transaction_uuid");
            String productCode = (String) data.get("product_code");
            String receivedSignature = (String) data.get("signature");
            
            // 1. Find Order using the UUID suffix (OrderId_Timestamp)
            String[] uuidParts = transactionUuid.split("_");
            Long orderId = Long.valueOf(uuidParts[0]);
            
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found for UUID: " + transactionUuid));
            
            // 2. Security Check: Verify eSewa's signature of the response
            // The format from eSewa callback is: transaction_code=XXX,status=COMPLETE,total_amount=X,transaction_uuid=YYY,product_code=ZZZ,signed_field_names=...
            String signedFieldNames = (String) data.get("signed_field_names");
            if (signedFieldNames != null && !signedFieldNames.isEmpty()) {
                String[] fields = signedFieldNames.split(",");
                StringBuilder messageBuilder = new StringBuilder();
                for (int i = 0; i < fields.length; i++) {
                    String field = fields[i].trim();
                    messageBuilder.append(field).append("=").append(data.get(field));
                    if (i < fields.length - 1) {
                        messageBuilder.append(",");
                    }
                }
                
                String expectedSignature = generateHmacSha256(messageBuilder.toString(), esewaSecretKey);
                
                if (!expectedSignature.equals(receivedSignature)) {
                    log.error("eSewa signature verification failed! Expected: {}, Got: {}", expectedSignature, receivedSignature);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "error", "Invalid signature - possible tampering"));
                }
            }
            
            log.info("eSewa verification for UUID {}: status={}, tc={}", transactionUuid, status, transactionCode);

            if ("COMPLETE".equalsIgnoreCase(status)) {
                // Mark order as paid and move to PROCESSING
                order.setPaymentReference(transactionCode);
                order.setStatus(OrderStatus.PROCESSING);
                orderRepository.save(order);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "status", status,
                        "transactionId", transactionCode
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "status", status,
                        "error", "Payment status is " + status
                ));
            }

        } catch (Exception e) {
            log.error("eSewa verification failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Verification process failed: " + e.getMessage()));
        }
    }
    
    /**
     * Helper method to generate HMAC-SHA256 Base64 hash
     */
    private String generateHmacSha256(String message, String secret) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
