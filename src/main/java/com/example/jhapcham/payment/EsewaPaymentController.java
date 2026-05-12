package com.example.jhapcham.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment/esewa")
@RequiredArgsConstructor
public class EsewaPaymentController {

    private final EsewaPaymentService esewaPaymentService;
    private final EsewaProperties esewaProperties;
    private final PaymentFrontendProperties paymentFrontendProperties;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @PostMapping("/signature")
    public ResponseEntity<?> getSignature(@RequestBody Map<String, Object> request, Authentication authentication) {
        String amount = String.valueOf(request.get("amount"));
        String transactionUuid = String.valueOf(request.get("transactionUuid"));
        List<Long> orderIds = extractOrderIds(request.get("orderIds"));

        esewaPaymentService.preparePayment(orderIds, amount, transactionUuid,
                currentUserService.requireUser(authentication));
        
        log.info("Generating eSewa signature for amount={} and uuid={}", amount, transactionUuid);
        String signature = esewaPaymentService.generateSignature(amount, transactionUuid);
        
        return ResponseEntity.ok(Map.of(
            "signature", signature,
            "productCode", esewaProperties.getProductCode(),
            "paymentUrl", esewaProperties.getPaymentUrl()
        ));
    }

    @PostMapping("/commission/signature")
    public ResponseEntity<?> getCommissionSignature(@RequestBody Map<String, Object> request, Authentication authentication) {
        String amount = String.valueOf(request.get("amount"));
        String transactionUuid = String.valueOf(request.get("transactionUuid"));
        List<Long> orderIds = extractOrderIds(request.get("orderIds"));

        esewaPaymentService.prepareCommissionPayment(orderIds, amount, transactionUuid,
                currentUserService.requireUser(authentication));
        
        log.info("Generating eSewa commission signature for amount={} and uuid={}", amount, transactionUuid);
        String signature = esewaPaymentService.generateSignature(amount, transactionUuid);
        
        return ResponseEntity.ok(Map.of(
            "signature", signature,
            "productCode", esewaProperties.getProductCode(),
            "paymentUrl", esewaProperties.getPaymentUrl()
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request, Authentication authentication) {
        String data = request.get("data");
        log.info("Received eSewa verification request");
        return ResponseEntity.ok(esewaPaymentService.processSuccess(data, currentUserService.requireUser(authentication)));
    }

    @GetMapping("/success")
    public RedirectView handleSuccess(@RequestParam String data) {
        // Fallback for direct browser redirect if needed
        return new RedirectView(frontendBaseUrl() + "/payment/success?data=" + data);
    }

    @GetMapping("/failure")
    public RedirectView handleFailure(@RequestParam(required = false) String data) {
        log.warn("Received eSewa failure callback");
        List<Long> orderIds = List.of();
        if (data != null) {
            try {
                String decodedJson = new String(java.util.Base64.getDecoder().decode(data));
                String transactionUuid = extractValue(decodedJson, "transaction_uuid");
                orderIds = extractOrderIdsFromTransactionUuid(transactionUuid);
            } catch (Exception e) {
                log.warn("Unable to decode eSewa failure payload", e);
            }
        }
        String query = orderIds.isEmpty() ? "" : "?orderIds=" + String.join(",", orderIds.stream().map(String::valueOf).toList());
        return new RedirectView(frontendBaseUrl() + "/payment/failure" + query);
    }

    private List<Long> extractOrderIds(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(value -> Long.parseLong(String.valueOf(value)))
                    .toList();
        }
        if (raw instanceof String text && !text.isBlank()) {
            return java.util.Arrays.stream(text.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(Long::parseLong)
                    .toList();
        }
        throw new com.example.jhapcham.Error.BusinessValidationException("Order IDs are required for eSewa payment");
    }

    private String frontendBaseUrl() {
        if (esewaProperties.getFrontendBaseUrl() != null && !esewaProperties.getFrontendBaseUrl().isBlank()) {
            return esewaProperties.getFrontendBaseUrl();
        }
        return paymentFrontendProperties.getBaseUrl();
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private List<Long> extractOrderIdsFromTransactionUuid(String transactionUuid) {
        if (transactionUuid == null || transactionUuid.isBlank()) {
            return List.of();
        }
        String[] parts = transactionUuid.split("-");
        if (parts.length < 2) {
            return List.of();
        }
        if ("ORDS".equalsIgnoreCase(parts[0])) {
            return java.util.Arrays.stream(parts[1].split("_"))
                    .map(Long::parseLong)
                    .toList();
        }
        return List.of(Long.parseLong(parts[1]));
    }
}
