//package com.example.jhapcham.order;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.client.RestTemplate;
//
//import java.math.BigDecimal;
//import java.util.*;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/payment/khalti")
//@RequiredArgsConstructor
//public class KhaltiPaymentController {
//
//    private final OrderRepository orderRepository;
//
//    @Value("${khalti.secret.key}")
//    private String khaltiSecretKey;
//
//    @Value("${khalti.api.url}")
//    private String khaltiApiUrl;
//
//    @Value("${khalti.return.url}")
//    private String returnUrl;
//
//    @Value("${khalti.website.url}")
//    private String websiteUrl;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    /**
//     * Step 1: Initiate Khalti payment for an existing order.
//     * POST /api/payment/khalti/initiate
//     * Body: { "orderId": 123 }
//     * Returns: { "paymentUrl": "https://pay.khalti.com/..." }
//     */
//    @PostMapping("/initiate")
//    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, Object> body) {
//        Long orderId = Long.valueOf(body.get("orderId").toString());
//
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
//
//        if (order.getPaymentMethod() != PaymentMethod.KHALTI) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Order is not set to Khalti payment"));
//        }
//
//        // Amount in paisa (1 NPR = 100 paisa)
//        long amountInPaisa = order.getGrandTotal()
//                .multiply(BigDecimal.valueOf(100))
//                .longValue();
//
//        // Build product_details list
//        List<Map<String, Object>> productDetails = new ArrayList<>();
//        for (OrderItem item : order.getItems()) {
//            Map<String, Object> pd = new HashMap<>();
//            pd.put("identity", String.valueOf(item.getProductIdSnapshot() != null ? item.getProductIdSnapshot() : item.getId()));
//            pd.put("name", item.getProductNameSnapshot() != null ? item.getProductNameSnapshot() : "Product");
//            pd.put("total_price", item.getLineTotal().multiply(BigDecimal.valueOf(100)).longValue());
//            pd.put("quantity", item.getQuantity());
//            pd.put("unit_price", item.getUnitPrice().multiply(BigDecimal.valueOf(100)).longValue());
//            productDetails.add(pd);
//        }
//
//        String orderName = order.getItems().isEmpty()
//                ? "Order #" + order.getId()
//                : order.getItems().get(0).getProductNameSnapshot();
//
//        // Build request payload
//        Map<String, Object> payload = new LinkedHashMap<>();
//        payload.put("return_url", returnUrl);
//        payload.put("website_url", websiteUrl);
//        payload.put("amount", amountInPaisa);
//        payload.put("purchase_order_id", String.valueOf(order.getId()));
//        payload.put("purchase_order_name", orderName);
//        payload.put("customer_info", Map.of(
//                "name", order.getCustomerName(),
//                "email", order.getCustomerEmail(),
//                "phone", order.getCustomerPhone()
//        ));
//        payload.put("product_details", productDetails);
//
//        // Set headers
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("Authorization", "Key " + khaltiSecretKey);
//
//        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
//
//        try {
//            ResponseEntity<Map> response = restTemplate.postForEntity(
//                    khaltiApiUrl + "epayment/initiate/",
//                    request,
//                    Map.class
//            );
//
//            Map<String, Object> responseBody = response.getBody();
//            if (responseBody == null || !responseBody.containsKey("payment_url")) {
//                log.error("Khalti initiate returned unexpected response: {}", responseBody);
//                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
//                        .body(Map.of("error", "Khalti did not return a payment URL"));
//            }
//
//            // Save pidx so we can verify later
//            String pidx = (String) responseBody.get("pidx");
//            order.setKhaltiPidx(pidx);
//            orderRepository.save(order);
//
//            log.info("Khalti payment initiated for order {}. pidx: {}", orderId, pidx);
//            return ResponseEntity.ok(Map.of(
//                    "paymentUrl", responseBody.get("payment_url"),
//                    "pidx", pidx
//            ));
//
//        } catch (Exception e) {
//            log.error("Khalti initiation failed for order {}: {}", orderId, e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", "Failed to initiate Khalti payment: " + e.getMessage()));
//        }
//    }
//
//    /**
//     * Step 2: Verify Khalti payment after user returns from pay.khalti.com
//     * POST /api/payment/khalti/verify
//     * Body: { "pidx": "...", "orderId": 123 }
//     * Returns: { "success": true, "transactionId": "..." }
//     */
//    @PostMapping("/verify")
//    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> body) {
//        String pidx = (String) body.get("pidx");
//        Long orderId = Long.valueOf(body.get("orderId").toString());
//
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("Authorization", "Key " + khaltiSecretKey);
//
//        Map<String, String> lookupPayload = Map.of("pidx", pidx);
//        HttpEntity<Map<String, String>> request = new HttpEntity<>(lookupPayload, headers);
//
//        try {
//            ResponseEntity<Map> response = restTemplate.postForEntity(
//                    khaltiApiUrl + "epayment/lookup/",
//                    request,
//                    Map.class
//            );
//
//            Map<String, Object> responseBody = response.getBody();
//            if (responseBody == null) {
//                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
//                        .body(Map.of("success", false, "error", "Khalti lookup returned empty response"));
//            }
//
//            String status = (String) responseBody.get("status");
//            String transactionId = (String) responseBody.get("transaction_id");
//
//            log.info("Khalti lookup for pidx {}: status={}, txId={}", pidx, status, transactionId);
//
//            if ("Completed".equalsIgnoreCase(status)) {
//                // Mark order as paid and move to PROCESSING
//                order.setPaymentReference(transactionId);
//                order.setStatus(OrderStatus.PROCESSING);
//                orderRepository.save(order);
//
//                return ResponseEntity.ok(Map.of(
//                        "success", true,
//                        "status", status,
//                        "transactionId", transactionId != null ? transactionId : ""
//                ));
//            } else {
//                return ResponseEntity.ok(Map.of(
//                        "success", false,
//                        "status", status,
//                        "error", "Payment not completed. Status: " + status
//                ));
//            }
//
//        } catch (Exception e) {
//            log.error("Khalti verification failed for pidx {}: {}", pidx, e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("success", false, "error", "Verification failed: " + e.getMessage()));
//        }
//    }
//}
