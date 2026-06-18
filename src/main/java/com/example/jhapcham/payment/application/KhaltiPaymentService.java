package com.example.jhapcham.payment.application;


import com.example.jhapcham.payment.application.*;
import com.example.jhapcham.payment.domain.*;
import com.example.jhapcham.payment.dto.*;
import com.example.jhapcham.payment.persistence.*;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.loyalty.domain.LoyaltyDomainEvent;
import com.example.jhapcham.loyalty.domain.LoyaltyEventType;
import com.example.jhapcham.order.domain.CommissionStatus;
import com.example.jhapcham.order.domain.Order;
import com.example.jhapcham.order.application.OrderAccountingService;
import com.example.jhapcham.order.persistence.OrderRepository;
import com.example.jhapcham.order.domain.OrderStatus;
import com.example.jhapcham.order.application.OrderStockService;
import com.example.jhapcham.order.domain.PaymentMethod;
import com.example.jhapcham.order.domain.PaymentStatus;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class KhaltiPaymentService {

    private final KhaltiProperties khaltiProperties;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentWorkflowService paymentWorkflowService;
    private final OrderStockService orderStockService;
    private final OrderAccountingService orderAccountingService;
    private final RestTemplate restTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Map<String, Object> initiateOrderPayment(List<Long> orderIds, BigDecimal requestedAmount, String purchaseOrderId, User actor) {
        ensureConfigured();
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BusinessValidationException("At least one order is required for Khalti payment");
        }
        if (purchaseOrderId == null || purchaseOrderId.isBlank()) {
            throw new BusinessValidationException("Purchase order ID is required");
        }

        List<Order> orders = orderRepository.findAllByIdForPaymentUpdate(orderIds);
        if (orders.size() != orderIds.size()) {
            throw new ResourceNotFoundException("One or more Khalti orders were not found");
        }
        paymentWorkflowService.assertCustomerPaymentOwnership(orders, actor);

        BigDecimal actualTotal = orders.stream()
                .map(order -> order.getGrandTotal() != null ? order.getGrandTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (actualTotal.compareTo(requestedAmount.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new BusinessValidationException("Payment amount does not match order total");
        }

        LocalDateTime now = LocalDateTime.now();
        for (Order order : orders) {
            if (order.getPaymentMethod() != PaymentMethod.KHALTI) {
                throw new BusinessValidationException("Order #" + order.getId() + " is not a Khalti order");
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
        }

        Map<String, Object> response = initiateWithKhalti(Map.of(
                "return_url", backendBaseUrl() + "/api/payment/khalti/order/success",
                "website_url", khaltiProperties.getWebsiteUrl(),
                "amount", toPaisa(actualTotal),
                "purchase_order_id", purchaseOrderId,
                "purchase_order_name", "Jhapcham Order " + orderIds,
                "customer_info", Map.of(
                        "name", safeCustomerName(orders),
                        "email", safeCustomerEmail(orders),
                        "phone", safeCustomerPhone(orders)
                )
        ));

        String pidx = String.valueOf(response.get("pidx"));
        String paymentUrl = String.valueOf(response.get("payment_url"));
        for (Order order : orders) {
            Payment payment = paymentRepository.findByOrderForUpdate(order)
                    .orElseGet(() -> Payment.builder()
                            .order(order)
                            .method(PaymentMethod.KHALTI)
                            .amount(order.getGrandTotal())
                            .createdAt(now)
                            .build());
            payment.setState(PaymentState.INITIATED);
            payment.setAmount(order.getGrandTotal());
            payment.setTransactionUuid(orderSpecificPurchaseOrderId(purchaseOrderId, order.getId(), orderIds));
            payment.setProviderReferenceId(pidx);
            payment.setInitiatedAt(now);
            payment.setUpdatedAt(now);
            Payment saved = paymentRepository.save(payment);
            paymentWorkflowService.recordEvent(saved, PaymentEventType.INITIATE_RESPONSE, String.valueOf(response));
        }

        return Map.of(
                "pidx", pidx,
                "paymentUrl", paymentUrl,
                "purchaseOrderId", purchaseOrderId,
                "orderIds", orderIds,
                "amount", actualTotal.toPlainString());
    }

    @Transactional
    public Map<String, Object> initiateCommissionPayment(List<Long> orderIds, BigDecimal requestedAmount, String purchaseOrderId, User actor) {
        ensureConfigured();
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BusinessValidationException("At least one order is required for commission payment");
        }

        List<Order> orders = orderRepository.findAllByIdForPaymentUpdate(orderIds);
        if (orders.size() != orderIds.size()) {
            throw new ResourceNotFoundException("One or more orders were not found");
        }
        assertCommissionOwnership(orders, actor);

        BigDecimal actualTotal = commissionTotal(orders);
        if (actualTotal.compareTo(requestedAmount.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new BusinessValidationException("Commission payment amount does not match calculated total");
        }

        Map<String, Object> response = initiateWithKhalti(Map.of(
                "return_url", backendBaseUrl() + "/api/payment/khalti/commission/success",
                "website_url", khaltiProperties.getWebsiteUrl(),
                "amount", toPaisa(actualTotal),
                "purchase_order_id", purchaseOrderId,
                "purchase_order_name", "Jhapcham Commission " + orderIds,
                "customer_info", Map.of(
                        "name", actor != null && actor.getFullName() != null ? actor.getFullName() : "Jhapcham Seller",
                        "email", actor != null && actor.getEmail() != null ? actor.getEmail() : "seller@jhapcham.local",
                        "phone", actor != null && actor.getContactNumber() != null ? actor.getContactNumber() : "9800000000"
                )
        ));

        return Map.of(
                "pidx", String.valueOf(response.get("pidx")),
                "paymentUrl", String.valueOf(response.get("payment_url")),
                "purchaseOrderId", purchaseOrderId,
                "orderIds", orderIds,
                "amount", actualTotal.toPlainString());
    }

    @Transactional
    public Map<String, Object> verifyOrderCallback(String pidx, String purchaseOrderId, String status) {
        ensureCompletedStatus(status);
        Map<String, Object> lookup = lookup(pidx);
        ensureLookupCompleted(lookup);
        List<Long> orderIds = extractOrderIds(purchaseOrderId);
        List<Order> orders = orderRepository.findAllByIdForPaymentUpdate(orderIds);
        if (orders.size() != orderIds.size()) {
            throw new ResourceNotFoundException("One or more Khalti orders were not found");
        }

        BigDecimal paidAmount = paisaToRupees(lookup.get("total_amount"));
        BigDecimal expectedTotal = orders.stream()
                .map(order -> order.getGrandTotal() != null ? order.getGrandTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (expectedTotal.compareTo(paidAmount) != 0) {
            markVerificationFailed(orders, "Khalti paid amount mismatch");
            throw new BusinessValidationException("Khalti paid amount does not match order total");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Long> refundPendingOrderIds = new ArrayList<>();
        String transactionId = String.valueOf(lookup.get("transaction_id"));
        for (Order order : orders) {
            if (order.getPaymentMethod() != PaymentMethod.KHALTI) {
                throw new BusinessValidationException("Order #" + order.getId() + " is not a Khalti order");
            }

            Payment payment = paymentRepository.findByOrderForUpdate(order)
                    .orElseThrow(() -> new BusinessValidationException("Khalti payment was not initiated for order #" + order.getId()));
            if (!Objects.equals(payment.getProviderReferenceId(), pidx)) {
                throw new BusinessValidationException("Stale Khalti callback for order #" + order.getId());
            }
            if (payment.getState() == PaymentState.SUCCESS && order.getPaymentStatus() == PaymentStatus.PAID) {
                continue;
            }

            order.setPaymentReference(transactionId);
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
                order.setPaymentStatus(PaymentStatus.PAID);
                if (order.getStatus() == OrderStatus.DRAFT || order.getStatus() == OrderStatus.PENDING) {
                    order.setStatus(OrderStatus.CONFIRMED);
                }
            }
            orderRepository.save(order);

            payment.setState(PaymentState.SUCCESS);
            payment.setProviderReferenceId(pidx);
            payment.setCompletedAt(now);
            payment.setUpdatedAt(now);
            Payment saved = paymentRepository.save(payment);
            paymentWorkflowService.recordEvent(saved, PaymentEventType.VERIFICATION_SUCCEEDED, String.valueOf(lookup));
            if (order.getUser() != null) {
                eventPublisher.publishEvent(new LoyaltyDomainEvent(LoyaltyEventType.PAYMENT_COMPLETED,
                        order.getId(), order.getUser().getId(), "payment-completed-" + order.getId()));
            }
        }

        return Map.of(
                "message", refundPendingOrderIds.isEmpty()
                        ? "Khalti payment verified successfully"
                        : "Khalti payment verified, but one or more expired orders require refund review",
                "orderIds", orderIds,
                "refundPendingOrderIds", refundPendingOrderIds,
                "transactionCode", transactionId,
                "amount", paidAmount.toPlainString(),
                "referenceId", pidx);
    }

    @Transactional
    public Map<String, Object> verifyCommissionCallback(String pidx, String purchaseOrderId, String status) {
        ensureCompletedStatus(status);
        Map<String, Object> lookup = lookup(pidx);
        ensureLookupCompleted(lookup);
        List<Long> orderIds = extractOrderIds(purchaseOrderId);
        List<Order> orders = orderRepository.findAllByIdForPaymentUpdate(orderIds);
        if (orders.size() != orderIds.size()) {
            throw new ResourceNotFoundException("One or more commission orders were not found");
        }

        BigDecimal paidAmount = paisaToRupees(lookup.get("total_amount"));
        BigDecimal expectedTotal = commissionTotal(orders);
        if (expectedTotal.compareTo(paidAmount) != 0) {
            throw new BusinessValidationException("Khalti paid commission amount mismatch");
        }

        LocalDateTime now = LocalDateTime.now();
        String transactionId = String.valueOf(lookup.get("transaction_id"));
        for (Order order : orders) {
            order.setCommissionPaymentReference(transactionId);
            order.setCommissionPaidAt(now);
            orderAccountingService.markCommissionAsPaid(order);
            orderRepository.save(order);
        }

        return Map.of(
                "message", "Khalti commission payment verified successfully",
                "orderIds", orderIds,
                "referenceId", transactionId);
    }

    private Map<String, Object> initiateWithKhalti(Map<String, Object> payload) {
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers());
        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl("/epayment/initiate/"),
                HttpMethod.POST,
                entity,
                Map.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessValidationException("Unable to initiate Khalti payment");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        return body;
    }

    private Map<String, Object> lookup(String pidx) {
        if (pidx == null || pidx.isBlank()) {
            throw new BusinessValidationException("Khalti pidx is required");
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("pidx", pidx), headers());
        ResponseEntity<Map> response = restTemplate.exchange(apiUrl("/epayment/lookup/"), HttpMethod.POST, entity, Map.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessValidationException("Unable to verify Khalti payment");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        return body;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Key " + khaltiProperties.getSecretKey().trim());
        return headers;
    }

    private String apiUrl(String path) {
        String base = khaltiProperties.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    private void ensureConfigured() {
        if (!khaltiProperties.isEnabled()) {
            throw new BusinessValidationException("Khalti payment is disabled");
        }
        if (khaltiProperties.getSecretKey() == null || khaltiProperties.getSecretKey().isBlank()) {
            throw new BusinessValidationException("Khalti payment is not configured");
        }
    }

    private void ensureCompletedStatus(String status) {
        if (!"Completed".equalsIgnoreCase(status)) {
            throw new BusinessValidationException("Khalti payment was not completed");
        }
    }

    private void ensureLookupCompleted(Map<String, Object> lookup) {
        if (!"Completed".equalsIgnoreCase(String.valueOf(lookup.get("status")))) {
            throw new BusinessValidationException("Khalti payment lookup is not complete");
        }
    }

    private BigDecimal commissionTotal(List<Order> orders) {
        BigDecimal total = BigDecimal.ZERO;
        for (Order order : orders) {
            if (order.getCommissionStatus() == CommissionStatus.PAID) {
                throw new BusinessValidationException("Commission for order #" + order.getId() + " is already paid");
            }
            BigDecimal commission = order.getMarketplaceCommission() != null ? order.getMarketplaceCommission() : BigDecimal.ZERO;
            total = total.add(commission).add(calculateCurrentFine(order));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
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

    private void assertCommissionOwnership(List<Order> orders, User actor) {
        if (actor == null) {
            throw new BusinessValidationException("Seller authentication is required");
        }
        for (Order order : orders) {
            boolean belongsToSeller = order.getItems().stream()
                    .anyMatch(i -> i.getProduct() != null
                            && i.getProduct().getSellerProfile() != null
                            && i.getProduct().getSellerProfile().getUser() != null
                            && i.getProduct().getSellerProfile().getUser().getId().equals(actor.getId()));
            if (!belongsToSeller) {
                throw new BusinessValidationException("You do not have permission to pay commission for order #" + order.getId());
            }
            if (order.getCommissionStatus() == CommissionStatus.PAID) {
                throw new BusinessValidationException("Commission for order #" + order.getId() + " is already paid");
            }
        }
    }

    private void markVerificationFailed(List<Order> orders, String reason) {
        paymentWorkflowService.markVerificationFailed(orders, reason, false);
    }

    private List<Long> extractOrderIds(String purchaseOrderId) {
        if (purchaseOrderId == null || purchaseOrderId.isBlank()) {
            return List.of();
        }
        String[] parts = purchaseOrderId.split("-");
        if (parts.length < 2) {
            return List.of();
        }
        String idsPart = parts[1];
        return java.util.Arrays.stream(idsPart.split("_"))
                .filter(value -> !value.isBlank())
                .map(Long::parseLong)
                .toList();
    }

    private String orderSpecificPurchaseOrderId(String purchaseOrderId, Long orderId, List<Long> allOrderIds) {
        return allOrderIds.size() == 1 ? purchaseOrderId : purchaseOrderId + "-ORDER-" + orderId;
    }

    private Integer toPaisa(BigDecimal rupees) {
        return rupees.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private BigDecimal paisaToRupees(Object paisa) {
        return new BigDecimal(String.valueOf(paisa))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String backendBaseUrl() {
        String base = khaltiProperties.getBackendBaseUrl();
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base;
    }

    private String safeCustomerName(List<Order> orders) {
        return firstNonBlank(orders.get(0).getCustomerName(), "Jhapcham Customer");
    }

    private String safeCustomerEmail(List<Order> orders) {
        return firstNonBlank(orders.get(0).getCustomerEmail(), "customer@jhapcham.local");
    }

    private String safeCustomerPhone(List<Order> orders) {
        return firstNonBlank(orders.get(0).getCustomerPhone(), "9800000000");
    }

    private String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : fallback;
    }
}
