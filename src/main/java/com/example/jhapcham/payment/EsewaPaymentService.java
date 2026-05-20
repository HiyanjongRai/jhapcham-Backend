package com.example.jhapcham.payment;

import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.loyalty.LoyaltyDomainEvent;
import com.example.jhapcham.loyalty.LoyaltyEventType;
import com.example.jhapcham.order.CommissionStatus;
import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderAccountingService;
import com.example.jhapcham.order.OrderRepository;
import com.example.jhapcham.order.OrderStatus;
import com.example.jhapcham.order.OrderStockService;
import com.example.jhapcham.order.PaymentMethod;
import com.example.jhapcham.order.PaymentStatus;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class EsewaPaymentService {

    private final EsewaProperties esewaProperties;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final OrderStockService orderStockService;
    private final OrderAccountingService orderAccountingService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Generates a signature for eSewa v2.
     * Signed fields: total_amount,transaction_uuid,product_code
     */
    public String generateSignature(String totalAmount, String transactionUuid) {
        ensureEsewaConfigured();
        String formattedAmount = formatAmount(totalAmount);
        String signedData = String.format("total_amount=%s,transaction_uuid=%s,product_code=%s",
                formattedAmount,
                transactionUuid,
                esewaProperties.getProductCode().trim());
        
        log.debug("Generating eSewa signature. Data to sign: {}", signedData);
        return hmacSha256Base64(signedData);
    }

    @Transactional
    public Map<String, Object> preparePayment(List<Long> orderIds, String totalAmount, String transactionUuid) {
        return preparePayment(orderIds, totalAmount, transactionUuid, null);
    }

    @Transactional
    public Map<String, Object> preparePayment(List<Long> orderIds, String totalAmount, String transactionUuid,
            User actor) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BusinessValidationException("At least one order is required for eSewa payment");
        }
        if (transactionUuid == null || transactionUuid.isBlank()) {
            throw new BusinessValidationException("Transaction UUID is required");
        }

        BigDecimal expectedTotal = normalizeAmount(totalAmount);
        List<Order> orders = orderRepository.findAllByIdForPaymentUpdate(orderIds);
        if (orders.size() != orderIds.size()) {
            throw new ResourceNotFoundException("One or more eSewa orders were not found");
        }
        assertPaymentOwnership(orders, actor);

        BigDecimal actualTotal = orders.stream()
                .map(order -> order.getGrandTotal() != null ? order.getGrandTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (actualTotal.compareTo(expectedTotal) != 0) {
            throw new BusinessValidationException("Payment amount does not match order total");
        }

        LocalDateTime now = LocalDateTime.now();
        for (Order order : orders) {
            if (order.getPaymentMethod() != PaymentMethod.ESEWA) {
                throw new BusinessValidationException("Order #" + order.getId() + " is not an eSewa order");
            }
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                throw new BusinessValidationException("Order #" + order.getId() + " has already been paid");
            }
            if (order.getStatus() != OrderStatus.DRAFT) {
                throw new BusinessValidationException("Order #" + order.getId() + " is not available for payment");
            }

            orderStockService.deductStockForOrder(order);

            order.setPaymentStatus(PaymentStatus.PAYMENT_INITIATED);
            order.setPaymentInitiatedAt(now);
            orderRepository.save(order);

            Payment payment = paymentRepository.findByOrderForUpdate(order)
                    .orElseGet(() -> Payment.builder()
                            .order(order)
                            .method(order.getPaymentMethod())
                            .amount(order.getGrandTotal())
                            .createdAt(now)
                            .build());
            payment.setState(PaymentState.INITIATED);
            payment.setAmount(order.getGrandTotal());
            payment.setTransactionUuid(orderSpecificTransactionUuid(transactionUuid, order.getId(), orderIds));
            payment.setInitiatedAt(now);
            payment.setUpdatedAt(now);
            Payment saved = paymentRepository.save(payment);
            recordEvent(saved, PaymentEventType.INITIATE_REQUEST, "Prepared eSewa payment " + transactionUuid);
        }

        return Map.of(
                "transactionUuid", transactionUuid,
                "orderIds", orderIds,
                "amount", actualTotal.toPlainString());
    }
    @Transactional
    public Map<String, Object> prepareCommissionPayment(List<Long> orderIds, String totalAmount, String transactionUuid, User actor) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BusinessValidationException("At least one order is required for commission payment");
        }
        if (transactionUuid == null || transactionUuid.isBlank()) {
            throw new BusinessValidationException("Transaction UUID is required");
        }

        BigDecimal expectedTotal = normalizeAmount(totalAmount);
        List<Order> orders = orderRepository.findAllByIdForPaymentUpdate(orderIds);
        if (orders.size() != orderIds.size()) {
            throw new ResourceNotFoundException("One or more orders were not found");
        }

        // Verify ownership (the actor must be the seller of these orders)
        for (Order order : orders) {
             boolean belongsToSeller = order.getItems().stream()
                        .anyMatch(i -> i.getProduct() != null && i.getProduct().getSellerProfile() != null && 
                                       i.getProduct().getSellerProfile().getUser().getId().equals(actor.getId()));
             if (!belongsToSeller) {
                 throw new BusinessValidationException("You do not have permission to pay commission for order #" + order.getId());
             }
             if (order.getCommissionStatus() == CommissionStatus.PAID) {
                 throw new BusinessValidationException("Commission for order #" + order.getId() + " is already paid");
             }
        }

        BigDecimal actualTotal = BigDecimal.ZERO;
        for (Order order : orders) {
            BigDecimal commission = order.getMarketplaceCommission() != null ? order.getMarketplaceCommission() : BigDecimal.ZERO;
            actualTotal = actualTotal.add(commission).add(calculateCurrentFine(order));
        }
        actualTotal = actualTotal.setScale(2, RoundingMode.HALF_UP);

        if (actualTotal.compareTo(expectedTotal) != 0) {
             throw new BusinessValidationException("Commission payment amount does not match calculated total");
        }

        return Map.of(
                "transactionUuid", transactionUuid,
                "orderIds", orderIds,
                "amount", actualTotal.toPlainString());
    }

    private BigDecimal calculateCurrentFine(Order order) {
        BigDecimal fine = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        if (order.getCommissionDueDate() != null && now.isAfter(order.getCommissionDueDate())) {
            long daysLate = java.time.Duration.between(order.getCommissionDueDate(), now).toDays();
            long weeksLate = daysLate / 7;
            double multiplier = 0.10 + (weeksLate * 0.05);
            fine = order.getMarketplaceCommission().multiply(BigDecimal.valueOf(multiplier))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return fine;
    }

    @Transactional
    public Map<String, Object> processSuccess(String data) {
        return processSuccess(data, null);
    }

    @Transactional
    public Map<String, Object> processSuccess(String data, User actor) {
        ensureEsewaConfigured();
        if (data == null || data.isBlank()) {
            throw new BusinessValidationException("Missing eSewa response data");
        }

        try {
            String decodedJson = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
            log.info("eSewa success callback data: {}", decodedJson);

            Map<String, String> payload = objectMapper.readValue(decodedJson, new TypeReference<Map<String, String>>() {});
            String transactionUuid = payload.get("transaction_uuid");
            String status = payload.get("status");
            String refId = firstNonBlank(payload.get("transaction_code"), payload.get("ref_id"));
            BigDecimal paidAmount = normalizeAmount(payload.get("total_amount"));

            List<Long> orderIds = extractOrderIds(transactionUuid);
            if (orderIds.isEmpty()) {
                throw new BusinessValidationException("Invalid eSewa transaction UUID");
            }

            List<Order> orders = orderRepository.findAllByIdForPaymentUpdate(orderIds);
            if (orders.size() != orderIds.size()) {
                throw new ResourceNotFoundException("One or more paid orders were not found");
            }

            if (transactionUuid.startsWith("COMM-")) {
                 return processCommissionSuccess(payload, orders, decodedJson);
            }

            assertPaymentOwnership(orders, actor);

            Payment firstPayment = paymentRepository.findByOrderForUpdate(orders.get(0)).orElse(null);
            if (firstPayment != null) {
                recordEvent(firstPayment, PaymentEventType.CALLBACK_RECEIVED, decodedJson);
            }

            if (!verifyResponseSignature(payload)) {
                markVerificationFailed(orders, "Invalid eSewa response signature");
                throw new BusinessValidationException("Invalid eSewa response signature");
            }

            if (!"COMPLETE".equalsIgnoreCase(status)) {
                markVerificationFailed(orders, "eSewa status was not COMPLETE: " + status);
                throw new BusinessValidationException("eSewa payment is not complete");
            }

            if (!Objects.equals(esewaProperties.getProductCode().trim(), payload.get("product_code"))) {
                markVerificationFailed(orders, "Product code mismatch");
                throw new BusinessValidationException("eSewa product code mismatch");
            }

            BigDecimal expectedTotal = orders.stream()
                    .map(order -> order.getGrandTotal() != null ? order.getGrandTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            if (expectedTotal.compareTo(paidAmount) != 0) {
                markVerificationFailed(orders, "Paid amount mismatch");
                throw new BusinessValidationException("eSewa paid amount does not match order total");
            }

            LocalDateTime now = LocalDateTime.now();
            List<Long> refundPendingOrderIds = new ArrayList<>();
            for (Order order : orders) {
                if (order.getPaymentMethod() != PaymentMethod.ESEWA) {
                    throw new BusinessValidationException("Order #" + order.getId() + " is not an eSewa order");
                }

                Payment payment = paymentRepository.findByOrderForUpdate(order)
                        .orElseGet(() -> Payment.builder()
                                .order(order)
                                .method(order.getPaymentMethod())
                                .amount(order.getGrandTotal())
                                .createdAt(now)
                                .build());

                String expectedOrderTransactionUuid = orderSpecificTransactionUuid(transactionUuid, order.getId(), orderIds);
                if (payment.getTransactionUuid() != null
                        && !Objects.equals(payment.getTransactionUuid(), expectedOrderTransactionUuid)) {
                    throw new BusinessValidationException("Stale eSewa payment callback for order #" + order.getId());
                }
                if (payment.getState() == PaymentState.SUCCESS && order.getPaymentStatus() == PaymentStatus.PAID) {
                    log.info("Ignoring replayed successful eSewa callback for order {}", order.getId());
                    continue;
                }

                order.setPaymentReference(refId);
                if (order.getPaidAt() == null) {
                    order.setPaidAt(now);
                }

                if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.FAILED) {
                    order.setRefundPending(true);
                    if (order.getPaymentStatus() != PaymentStatus.REFUNDED) {
                        order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
                    }
                    refundPendingOrderIds.add(order.getId());
                } else {
                    orderStockService.deductStockForOrder(order);
                    if (order.getPaymentStatus() != PaymentStatus.REFUND_PENDING
                            && order.getPaymentStatus() != PaymentStatus.REFUNDED) {
                        order.setPaymentStatus(PaymentStatus.PAID);
                    }
                    if (order.getStatus() == OrderStatus.DRAFT || order.getStatus() == OrderStatus.PENDING) {
                        order.setStatus(OrderStatus.CONFIRMED);
                    }
                }

                orderRepository.save(order);

                payment.setState(PaymentState.SUCCESS);
                payment.setAmount(order.getGrandTotal());
                payment.setTransactionUuid(expectedOrderTransactionUuid);
                payment.setProviderReferenceId(refId);
                payment.setCompletedAt(now);
                payment.setUpdatedAt(now);
                Payment saved = paymentRepository.save(payment);
                recordEvent(saved, PaymentEventType.VERIFICATION_SUCCEEDED, decodedJson);
                if (order.getUser() != null) {
                    eventPublisher.publishEvent(new LoyaltyDomainEvent(LoyaltyEventType.PAYMENT_COMPLETED,
                            order.getId(), order.getUser().getId(), "payment-completed-" + order.getId()));
                }

                log.info("Order {} successfully paid via eSewa transaction {}", order.getId(), transactionUuid);
            }

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("message", refundPendingOrderIds.isEmpty()
                    ? "Payment verified successfully"
                    : "Payment verified, but one or more expired orders require refund review");
            result.put("orderIds", orderIds);
            result.put("refundPendingOrderIds", refundPendingOrderIds);
            result.put("transactionUuid", transactionUuid);
            result.put("transactionCode", refId != null ? refId : "");
            result.put("amount", paidAmount.toPlainString());
            result.put("referenceId", refId != null ? refId : "");
            return result;
        } catch (BusinessValidationException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing eSewa success callback", e);
            throw new BusinessValidationException("Payment verification failed");
        }
    }
    @Transactional
    public Map<String, Object> processCommissionSuccess(Map<String, String> payload, List<Order> orders, String decodedJson) {
        if (!verifyResponseSignature(payload)) {
            throw new BusinessValidationException("Invalid eSewa response signature for commission");
        }

        String refId = firstNonBlank(payload.get("transaction_code"), payload.get("ref_id"));
        BigDecimal paidAmount = normalizeAmount(payload.get("total_amount"));

        BigDecimal expectedTotal = BigDecimal.ZERO;
        for (Order order : orders) {
            BigDecimal commission = order.getMarketplaceCommission() != null ? order.getMarketplaceCommission() : BigDecimal.ZERO;
            expectedTotal = expectedTotal.add(commission).add(calculateCurrentFine(order));
        }
        expectedTotal = expectedTotal.setScale(2, RoundingMode.HALF_UP);

        if (expectedTotal.compareTo(paidAmount) != 0) {
            throw new BusinessValidationException("eSewa paid commission amount mismatch");
        }

        LocalDateTime now = LocalDateTime.now();
        for (Order order : orders) {
            order.setCommissionPaymentReference(refId);
            order.setCommissionPaidAt(now);
            orderAccountingService.markCommissionAsPaid(order);
            orderRepository.save(order);
            log.info("Commission for order {} successfully paid via eSewa transaction {}", order.getId(), refId);
        }

        return Map.of(
                "message", "Commission payment verified successfully",
                "orderIds", orders.stream().map(Order::getId).toList(),
                "referenceId", refId != null ? refId : "");
    }

    @Transactional
    public void processFailure(String orderIdRaw) {
        try {
            Long orderId = Long.parseLong(orderIdRaw);
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null
                    && order.getPaymentStatus() != PaymentStatus.PAID
                    && order.getStatus() == OrderStatus.DRAFT) {
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setStatus(OrderStatus.DRAFT);
                orderStockService.restoreStock(order);
                orderRepository.save(order);

                paymentRepository.findByOrder(order).ifPresent(payment -> {
                    payment.setState(PaymentState.FAILED);
                    payment.setFailureReason("Customer returned from eSewa failure URL");
                    payment.setUpdatedAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                    recordEvent(payment, PaymentEventType.PAYMENT_FAILURE, "Customer returned from failure URL");
                });
                log.warn("eSewa payment failed for order {}", orderId);
            }
        } catch (Exception e) {
            log.error("Error processing eSewa failure callback", e);
        }
    }

    private boolean verifyResponseSignature(Map<String, String> payload) {
        ensureEsewaConfigured();
        String receivedSignature = payload.get("signature");
        String signedFieldNames = payload.get("signed_field_names");
        if (receivedSignature == null || signedFieldNames == null) {
            log.warn("Missing signature or signed_field_names in eSewa response");
            return false;
        }

        List<String> fields = java.util.Arrays.asList(signedFieldNames.split(","));
        String signedData = buildSignedData(fields, payload);
        log.debug("Verifying eSewa response signature. Data to sign: {}", signedData);
        
        String expected = hmacSha256Base64(signedData);
        return constantTimeEquals(expected, receivedSignature);
    }

    private void assertPaymentOwnership(List<Order> orders, User actor) {
        if (actor == null || actor.getRole() == Role.ADMIN) {
            return;
        }
        for (Order order : orders) {
            if (order.getUser() == null || !order.getUser().getId().equals(actor.getId())) {
                throw new BusinessValidationException("You do not have permission to pay order #" + order.getId());
            }
        }
    }

    private String buildSignedData(List<String> fields, Map<String, String> values) {
        Map<String, String> ordered = new LinkedHashMap<>();
        for (String field : fields) {
            String cleanField = field.trim();
            ordered.put(cleanField, values.getOrDefault(cleanField, ""));
        }
        return ordered.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private String hmacSha256Base64(String data) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    esewaProperties.getSecretKey().trim().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("Error generating eSewa signature", e);
            throw new RuntimeException("Failed to generate payment signature");
        }
    }

    private void ensureEsewaConfigured() {
        if (!esewaProperties.isEnabled()) {
            throw new BusinessValidationException("eSewa payment is disabled");
        }
        if (esewaProperties.getProductCode() == null || esewaProperties.getProductCode().isBlank()
                || esewaProperties.getSecretKey() == null || esewaProperties.getSecretKey().isBlank()) {
            throw new BusinessValidationException("eSewa payment is not configured");
        }
    }

    private void markVerificationFailed(List<Order> orders, String reason) {
        for (Order order : orders) {
            if (order.getPaymentStatus() != PaymentStatus.PAID && order.getStatus() == OrderStatus.DRAFT) {
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setStatus(OrderStatus.DRAFT);
                orderRepository.save(order);
            }

            paymentRepository.findByOrder(order).ifPresent(payment -> {
                if (order.getPaymentStatus() != PaymentStatus.PAID && payment.getState() != PaymentState.SUCCESS) {
                    payment.setState(PaymentState.FAILED);
                    payment.setFailureReason(reason);
                    payment.setUpdatedAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }
                recordEvent(payment, PaymentEventType.VERIFICATION_FAILED, reason);
            });
        }
    }

    private void recordEvent(Payment payment, PaymentEventType type, String payload) {
        paymentEventRepository.save(PaymentEvent.builder()
                .payment(payment)
                .type(type)
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private String formatAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return "0.00";
        }
        return normalizeAmount(amount).toPlainString();
    }

    private BigDecimal normalizeAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(amount.replace(",", "").trim()).setScale(2, RoundingMode.HALF_UP);
    }

    private List<Long> extractOrderIds(String transactionUuid) {
        List<Long> ids = new ArrayList<>();
        if (transactionUuid == null || transactionUuid.isBlank()) {
            return ids;
        }

        String workUuid = transactionUuid;
        if (workUuid.startsWith("COMM-")) {
            workUuid = workUuid.substring(5);
        }

        String[] parts = workUuid.split("-");
        if (parts.length < 2) {
            return ids;
        }

        if ("ORDS".equalsIgnoreCase(parts[0])) {
            for (String id : parts[1].split("_")) {
                ids.add(Long.parseLong(id));
            }
            return ids;
        }

        if ("ORD".equalsIgnoreCase(parts[0])) {
            ids.add(Long.parseLong(parts[1]));
        }
        return ids;
    }

    private String orderSpecificTransactionUuid(String transactionUuid, Long orderId, List<Long> orderIds) {
        if (orderIds.size() == 1 || Objects.equals(orderIds.get(0), orderId)) {
            return transactionUuid;
        }
        return transactionUuid + "#" + orderId;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] left = expected.getBytes(StandardCharsets.UTF_8);
        byte[] right = actual.getBytes(StandardCharsets.UTF_8);
        if (left.length != right.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length; i++) {
            result |= left[i] ^ right[i];
        }
        return result == 0;
    }
}
