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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.getPaymentMethod() != PaymentMethod.ESEWA) {
            return ResponseEntity.badRequest().body(Map.of("error", "Order is not set to eSewa payment"));
        }

        // Format to 1 decimal place (e.g., 100.0) - standard for eSewa sandbox
        BigDecimal amountDec = order.getGrandTotal().setScale(1, java.math.RoundingMode.HALF_UP);
        String formattedAmount = amountDec.toPlainString();
        
        String transactionUuid = order.getId() + "_" + System.currentTimeMillis();
        String secret = esewaSecretKey.trim();
        String pCode = esewaProductCode.trim();

        // Sign all 5 fields for maximum compatibility
        // Format: amount=10.0,tax_amount=0.0,total_amount=10.0,transaction_uuid=123_456,product_code=EPAYTEST
        String message = String.format("amount=%s,tax_amount=%s,total_amount=%s,transaction_uuid=%s,product_code=%s",
                formattedAmount, "0.0", formattedAmount, transactionUuid, pCode);
        
        String signature;
        try {
            signature = generateHmacSha256(message, secret);
        } catch (Exception e) {
            log.error("Failed to generate eSewa signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate payment signature"));
        }

        order.setEsewaTransactionUuid(transactionUuid);
        orderRepository.save(order);

        Map<String, String> payload = new HashMap<>();
        payload.put("amount", formattedAmount);
        payload.put("tax_amount", "0.0");
        payload.put("total_amount", formattedAmount);
        payload.put("transaction_uuid", transactionUuid);
        payload.put("product_code", pCode);
        payload.put("product_service_charge", "0");
        payload.put("product_delivery_charge", "0");
        
        // Standard sandbox callback URLs
        payload.put("success_url", "http://localhost:3000/payment/esewa-callback");
        payload.put("failure_url", "http://localhost:3000/payment/esewa-callback");
        payload.put("signed_field_names", "amount,tax_amount,total_amount,transaction_uuid,product_code");
        payload.put("signature", signature);
        payload.put("epayUrl", esewaApiUrl);

        log.info("eSewa payment initiated for order {}. UUID: {}", orderId, transactionUuid);
        return ResponseEntity.ok(payload);
    }

    @Transactional
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> body) {
        String base64Data = (String) body.get("data");
        if (base64Data == null || base64Data.isEmpty()) {
             return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No data provided"));
        }

        try {
            String decodedStr = new String(Base64.getDecoder().decode(base64Data), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(decodedStr, Map.class);
            log.info("eSewa Customer Callback Data: {}", data);
            
            String transactionCode = (String) data.get("transaction_code");
            String status = (String) data.get("status");
            String transactionUuid = (String) data.get("transaction_uuid");
            String receivedSignature = (String) data.get("signature");
            
            String[] uuidParts = transactionUuid.split("_");
            Long orderId = Long.valueOf(uuidParts[0]);
            
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found for UUID: " + transactionUuid));
            
            String signedFieldNames = (String) data.get("signed_field_names");
            if (signedFieldNames != null && !signedFieldNames.isEmpty()) {
                String[] fields = signedFieldNames.split(",");
                StringBuilder messageBuilder = new StringBuilder();
                for (int i = 0; i < fields.length; i++) {
                    String field = fields[i].trim();
                    messageBuilder.append(field).append("=").append(data.get(field));
                    if (i < fields.length - 1) messageBuilder.append(",");
                }
                
                String expectedSignature = generateHmacSha256(messageBuilder.toString(), esewaSecretKey.trim());
                if (!expectedSignature.equals(receivedSignature)) {
                    log.error("eSewa signature verification failed! Expected: {}, Got: {}", expectedSignature, receivedSignature);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "error", "Invalid signature"));
                }
            }
            
            if ("COMPLETE".equalsIgnoreCase(status)) {
                order.setPaymentReference(transactionCode);
                order.setStatus(OrderStatus.PROCESSING);
                orderRepository.save(order);

                return ResponseEntity.ok(Map.of("success", true, "status", status, "transactionId", transactionCode));
            } else {
                return ResponseEntity.ok(Map.of("success", false, "status", status));
            }

        } catch (Exception e) {
            log.error("eSewa verification failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Verification process failed: " + e.getMessage()));
        }
    }
    
    private String generateHmacSha256(String message, String secret) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
