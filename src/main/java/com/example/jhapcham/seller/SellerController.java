package com.example.jhapcham.seller;

import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.order.CommissionStatus;
import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${esewa.secret.key}")
    private String esewaSecretKey;

    @Value("${esewa.product.code}")
    private String esewaProductCode;

    @Value("${esewa.api.url}")
    private String esewaApiUrl;

    @GetMapping("/{sellerUserId}")
    public ResponseEntity<?> getSeller(@PathVariable Long sellerUserId) {
        return ResponseEntity.ok(sellerService.getSellerProfile(sellerUserId));
    }

    @PutMapping("/{sellerUserId}")
    public ResponseEntity<SellerProfileResponseDTO> updateSeller(
            @PathVariable Long sellerUserId,
            @ModelAttribute SellerUpdateRequestDTO dto) {
        return ResponseEntity.ok(sellerService.updateSeller(sellerUserId, dto));
    }

    @GetMapping("/{sellerUserId}/income")
    public ResponseEntity<?> getSellerIncome(@PathVariable Long sellerUserId) {
        try {
            return ResponseEntity.ok(sellerService.getSellerIncome(sellerUserId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{sellerUserId}/stats")
    public ResponseEntity<?> getDashboardStats(@PathVariable Long sellerUserId) {
        return ResponseEntity.ok(sellerService.getDashboardStats(sellerUserId));
    }

    @GetMapping("/{sellerUserId}/commissions")
    public ResponseEntity<?> getSellerCommissions(@PathVariable Long sellerUserId) {
        return ResponseEntity.ok(sellerService.getSellerCommissions(sellerUserId));
    }

    @PostMapping("/{sellerUserId}/commissions/{orderId}/pay")
    public ResponseEntity<?> payCommission(@PathVariable Long sellerUserId, @PathVariable Long orderId) {
        try {
            sellerService.payCommission(sellerUserId, orderId);
            return ResponseEntity.ok(Map.of("message", "Commission paid successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{sellerUserId}/commissions/{orderId}/esewa/initiate")
    public ResponseEntity<?> initiateEsewaCommission(@PathVariable Long sellerUserId, @PathVariable Long orderId, @RequestBody Map<String, String> body) {
        try {
            String amountStr = body.get("amount");
            if (amountStr == null) return ResponseEntity.badRequest().build();
            
            // Format to 1 decimal place (e.g., 100.0) - standard for eSewa sandbox
            BigDecimal amountDec = new BigDecimal(amountStr).setScale(1, java.math.RoundingMode.HALF_UP);
            String formattedAmount = amountDec.toPlainString();
            
            String transactionUuid = "COMM_" + orderId + "_" + System.currentTimeMillis();
            String merchantSuccessUrl = "http://localhost:3000/seller/commissions";
            String secret = esewaSecretKey.trim();
            String pCode = esewaProductCode.trim();

            // Sign all 5 fields for maximum compatibility
            String message = String.format("amount=%s,tax_amount=%s,total_amount=%s,transaction_uuid=%s,product_code=%s", 
                    formattedAmount, "0.0", formattedAmount, transactionUuid, pCode);
            
            String signature = generateHmacSha256(message, secret);

            Map<String, String> payload = new HashMap<>();
            payload.put("amount", formattedAmount);
            payload.put("tax_amount", "0.0");
            payload.put("total_amount", formattedAmount);
            payload.put("transaction_uuid", transactionUuid);
            payload.put("product_code", pCode);
            payload.put("product_service_charge", "0");
            payload.put("product_delivery_charge", "0");
            payload.put("success_url", merchantSuccessUrl);
            payload.put("failure_url", merchantSuccessUrl);
            payload.put("signed_field_names", "amount,tax_amount,total_amount,transaction_uuid,product_code");
            payload.put("signature", signature);
            payload.put("epayUrl", esewaApiUrl);
            
            return ResponseEntity.ok(payload);
        } catch (Exception e) {
            log.error("Esewa initiate error", e);
            return ResponseEntity.status(500).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{sellerUserId}/commissions/pay-all/initiate")
    public ResponseEntity<?> initiateBulkEsewaCommission(@PathVariable Long sellerUserId, @RequestBody Map<String, String> body) {
        try {
            String amountStr = body.get("amount");
            if (amountStr == null) return ResponseEntity.badRequest().build();
            
            BigDecimal amountDec = new BigDecimal(amountStr).setScale(1, java.math.RoundingMode.HALF_UP);
            String formattedAmount = amountDec.toPlainString();
            
            String transactionUuid = "COMBULK_" + sellerUserId + "_" + System.currentTimeMillis();
            String merchantSuccessUrl = "http://localhost:3000/seller/commissions";
            String secret = esewaSecretKey.trim();
            String pCode = esewaProductCode.trim();

            String message = String.format("amount=%s,tax_amount=%s,total_amount=%s,transaction_uuid=%s,product_code=%s", 
                    formattedAmount, "0.0", formattedAmount, transactionUuid, pCode);
            
            String signature = generateHmacSha256(message, secret);

            Map<String, String> payload = new HashMap<>();
            payload.put("amount", formattedAmount);
            payload.put("tax_amount", "0.0");
            payload.put("total_amount", formattedAmount);
            payload.put("transaction_uuid", transactionUuid);
            payload.put("product_code", pCode);
            payload.put("product_service_charge", "0");
            payload.put("product_delivery_charge", "0");
            payload.put("success_url", merchantSuccessUrl);
            payload.put("failure_url", merchantSuccessUrl);
            payload.put("signed_field_names", "amount,tax_amount,total_amount,transaction_uuid,product_code");
            payload.put("signature", signature);
            payload.put("epayUrl", esewaApiUrl);
            
            return ResponseEntity.ok(payload);
        } catch (Exception e) {
            log.error("Esewa bulk initiate error", e);
            return ResponseEntity.status(500).body(new ErrorResponse(e.getMessage()));
        }
    }

    @Transactional
    @PostMapping("/commissions/esewa/verify")
    public ResponseEntity<?> verifyEsewaCommission(@RequestBody Map<String, String> body) {
        String base64Data = body.get("data");
        if (base64Data == null || base64Data.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("No verification data provided"));
        }

        try {
            String decodedStr = new String(Base64.getDecoder().decode(base64Data), StandardCharsets.UTF_8);
            Map<String, Object> data = objectMapper.readValue(decodedStr, Map.class);
            log.info("eSewa Seller Callback Data: {}", data);
            
            String status = (String) data.get("status");
            String transactionUuid = (String) data.get("transaction_uuid");
            String receivedSignature = (String) data.get("signature");

            String signedFieldNames = (String) data.get("signed_field_names");
            if (signedFieldNames != null) {
                String[] fields = signedFieldNames.split(",");
                StringBuilder msg = new StringBuilder();
                for (int i = 0; i < fields.length; i++) {
                    String f = fields[i].trim();
                    msg.append(f).append("=").append(data.get(f));
                    if (i < fields.length - 1) msg.append(",");
                }
                String expectedSignature = generateHmacSha256(msg.toString(), esewaSecretKey.trim());
                if (!expectedSignature.equals(receivedSignature)) {
                    log.error("Signature mismatch! Expected: {}, Received: {}", expectedSignature, receivedSignature);
                    return ResponseEntity.badRequest().body(new ErrorResponse("Signature mismatch"));
                }
            }

            if ("COMPLETE".equalsIgnoreCase(status)) {
                if (transactionUuid.startsWith("COMBULK_")) {
                    String[] parts = transactionUuid.split("_");
                    Long sellerId = Long.valueOf(parts[1]);
                    sellerService.payAllCommissions(sellerId);
                } else {
                    String[] parts = transactionUuid.split("_");
                    Long orderId = Long.valueOf(parts[1]);
                    Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
                    sellerService.payCommission(order.getItems().get(0).getProduct().getSellerProfile().getUser().getId(), orderId);
                }
                return ResponseEntity.ok(Map.of("success", true, "message", "Commission verified and paid"));
            }
            
            return ResponseEntity.ok(Map.of("success", false, "status", status));
        } catch (Exception e) {
            log.error("Verification error", e);
            return ResponseEntity.status(500).body(new ErrorResponse("Verification failed: " + e.getMessage()));
        }
    }

    private String generateHmacSha256(String message, String secret) throws Exception {
        log.info("Generating Signature for message: [{}]", message);
        javax.crypto.Mac sha256_HMAC = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secret_key = new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}