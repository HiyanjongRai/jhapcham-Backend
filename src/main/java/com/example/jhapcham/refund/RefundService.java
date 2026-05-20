package com.example.jhapcham.refund;

import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.common.FileStorageService;
import com.example.jhapcham.loyalty.LoyaltyDomainEvent;
import com.example.jhapcham.loyalty.LoyaltyEventType;
import com.example.jhapcham.notification.NotificationService;
import com.example.jhapcham.notification.NotificationType;
import com.example.jhapcham.order.*;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private static final int RETURN_WINDOW_DAYS = 7;
    private static final int MAX_GATEWAY_ATTEMPTS = 3;
    private static final EnumSet<RefundStatus> ACTIVE_OR_PAID_STATUSES = EnumSet.of(
            RefundStatus.REQUESTED,
            RefundStatus.UNDER_REVIEW,
            RefundStatus.APPROVED,
            RefundStatus.REFUND_PROCESSING,
            RefundStatus.REFUNDED);

    private final RefundRequestRepository refundRequestRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStockService orderStockService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final RefundGatewayService refundGatewayService;
    private final RefundStatusTransitionPolicy transitionPolicy;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RefundResponseDTO createRefund(User customer, RefundCreateRequestDTO request, List<MultipartFile> files) {
        if (customer.getRole() != Role.CUSTOMER && customer.getRole() != Role.ADMIN) {
            throw new AuthorizationException("Only customers can request refunds");
        }

        String idempotencyKey = normalizeIdempotencyKey(request.getIdempotencyKey());
        if (idempotencyKey != null) {
            Optional<RefundRequest> existing = refundRequestRepository.findByCustomerAndIdempotencyKey(customer, idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        validateCustomerOwnsOrder(customer, order);
        validateRefundEligibility(order);

        List<OrderItem> requestedItems = new ArrayList<>();
        Map<Long, RefundLineItemRequestDTO> itemRequests = new LinkedHashMap<>();
        for (RefundLineItemRequestDTO line : request.getItems()) {
            if (itemRequests.put(line.getOrderItemId(), line) != null) {
                throw new BusinessValidationException("Duplicate order item in refund request: " + line.getOrderItemId());
            }
            OrderItem item = orderItemRepository.findById(line.getOrderItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order item not found: " + line.getOrderItemId()));
            if (!item.getOrder().getId().equals(order.getId())) {
                throw new BusinessValidationException("Order item " + item.getId() + " does not belong to order " + order.getId());
            }
            if (refundRequestRepository.existsActiveForOrderItem(item.getId(), ACTIVE_OR_PAID_STATUSES)) {
                throw new BusinessValidationException("A refund already exists for item: " + item.getProductNameSnapshot());
            }
            if (line.getQuantity() > item.getQuantity()) {
                throw new BusinessValidationException("Refund quantity exceeds purchased quantity for " + item.getProductNameSnapshot());
            }
            requestedItems.add(item);
        }

        User seller = resolveSeller(requestedItems);
        RefundRequest refund = RefundRequest.builder()
                .order(order)
                .customer(customer)
                .seller(seller)
                .reason(request.getReason())
                .reasonDetails(request.getReasonDetails())
                .status(RefundStatus.REQUESTED)
                .paymentMethod(order.getPaymentMethod())
                .idempotencyKey(idempotencyKey)
                .shippingIncluded(request.isIncludeShipping())
                .customerNotes(request.getReasonDetails())
                .itemSubtotal(BigDecimal.ZERO)
                .taxRefund(BigDecimal.ZERO)
                .shippingRefund(BigDecimal.ZERO)
                .discountAdjustment(BigDecimal.ZERO)
                .totalRefund(BigDecimal.ZERO)
                .sellerCommissionReversal(BigDecimal.ZERO)
                .fraudScore(0)
                .fraudFlagged(false)
                .deleted(false)
                .submittedAt(LocalDateTime.now())
                .build();

        BigDecimal itemSubtotal = BigDecimal.ZERO;
        BigDecimal taxRefund = BigDecimal.ZERO;
        BigDecimal discountAdjustment = BigDecimal.ZERO;
        BigDecimal commissionReversal = BigDecimal.ZERO;

        for (OrderItem item : requestedItems) {
            RefundLineItemRequestDTO lineRequest = itemRequests.get(item.getId());
            RefundLineItem line = calculateLineItem(order, item, lineRequest);
            refund.addLineItem(line);
            itemSubtotal = itemSubtotal.add(line.getItemSubtotal());
            taxRefund = taxRefund.add(line.getTaxRefund());
            discountAdjustment = discountAdjustment.add(line.getDiscountAdjustment());
            commissionReversal = commissionReversal.add(line.getSellerCommissionReversal());
        }

        BigDecimal shippingRefund = calculateShippingRefund(order, refund.getLineItems(), request.isIncludeShipping());
        BigDecimal total = itemSubtotal.add(taxRefund).add(shippingRefund).subtract(discountAdjustment);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        refund.setItemSubtotal(scale(itemSubtotal));
        refund.setTaxRefund(scale(taxRefund));
        refund.setShippingRefund(scale(shippingRefund));
        refund.setDiscountAdjustment(scale(discountAdjustment));
        refund.setSellerCommissionReversal(scale(commissionReversal));
        refund.setTotalRefund(scale(total));
        refund.addHistory(history(null, RefundStatus.REQUESTED, customer, RefundActorType.CUSTOMER,
                "Refund request submitted"));

        RefundRequest saved = refundRequestRepository.save(refund);
        attachEvidence(saved, customer, files, "Customer refund evidence");
        applyFraudSignals(saved);

        notificationService.createNotification(seller, "New refund request",
                "Refund request #" + saved.getId() + " requires seller review.",
                NotificationType.REPORT_ALERT, saved.getId());

        return toResponse(saved);
    }

    @Transactional
    public RefundResponseDTO cancelRefund(Long refundId, User customer, String note) {
        RefundRequest refund = getForUpdate(refundId);
        if (!refund.getCustomer().getId().equals(customer.getId()) && customer.getRole() != Role.ADMIN) {
            throw new AuthorizationException("You cannot cancel this refund request");
        }
        if (refund.getStatus() != RefundStatus.REQUESTED && refund.getStatus() != RefundStatus.UNDER_REVIEW) {
            throw new BusinessValidationException("Refund can only be cancelled before approval");
        }
        transition(refund, RefundStatus.CANCELLED, customer,
                customer.getRole() == Role.ADMIN ? RefundActorType.ADMIN : RefundActorType.CUSTOMER,
                note != null ? note : "Refund request cancelled");
        refund.setCancelledAt(LocalDateTime.now());
        return toResponse(refundRequestRepository.save(refund));
    }

    @Transactional
    public RefundResponseDTO sellerReview(Long refundId, User seller, RefundReviewRequestDTO review, List<MultipartFile> files) {
        RefundRequest refund = getForUpdate(refundId);
        if (!refund.getSeller().getId().equals(seller.getId())) {
            throw new AuthorizationException("Only the seller for this order can review this refund");
        }
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new BusinessValidationException("Only requested refunds can be reviewed by seller");
        }
        refund.setSellerNotes(review.getNote());
        refund.setReviewedAt(LocalDateTime.now());
        attachEvidence(refund, seller, files, "Seller refund evidence");
        if (review.isApproved()) {
            transition(refund, RefundStatus.UNDER_REVIEW, seller, RefundActorType.SELLER,
                    "Seller approved refund for admin/payment review: " + review.getNote());
            notificationService.createNotification(refund.getCustomer(), "Refund under review",
                    "Seller approved your refund request #" + refund.getId() + ". Admin payment review is next.",
                    NotificationType.ORDER_UPDATE, refund.getId());
        } else {
            transition(refund, RefundStatus.REJECTED, seller, RefundActorType.SELLER,
                    "Seller rejected refund: " + review.getNote());
            notificationService.createNotification(refund.getCustomer(), "Refund rejected",
                    "Seller rejected refund request #" + refund.getId() + ". You may open a dispute if needed.",
                    NotificationType.ORDER_UPDATE, refund.getId());
        }
        return toResponse(refundRequestRepository.save(refund));
    }

    @Transactional
    public RefundResponseDTO adminApprove(Long refundId, User admin, RefundReviewRequestDTO review) {
        requireAdmin(admin);
        RefundRequest refund = getForUpdate(refundId);
        if (review.isApproved()) {
            if (refund.getStatus() == RefundStatus.REQUESTED) {
                transition(refund, RefundStatus.UNDER_REVIEW, admin, RefundActorType.ADMIN,
                        "Admin moved request to review");
            }
            if (refund.getStatus() != RefundStatus.UNDER_REVIEW) {
                throw new BusinessValidationException("Only under-review refunds can be approved");
            }
            refund.setAdminNotes(review.getNote());
            transition(refund, RefundStatus.APPROVED, admin, RefundActorType.ADMIN, review.getNote());
            refund.setApprovedAt(LocalDateTime.now());
            startRefundProcessing(refund, admin);
        } else {
            if (refund.getStatus() != RefundStatus.REQUESTED && refund.getStatus() != RefundStatus.UNDER_REVIEW) {
                throw new BusinessValidationException("Only pending refunds can be rejected");
            }
            refund.setAdminNotes(review.getNote());
            transition(refund, RefundStatus.REJECTED, admin, RefundActorType.ADMIN, review.getNote());
        }
        return toResponse(refundRequestRepository.save(refund));
    }

    @Transactional
    public RefundResponseDTO confirmGatewayRefund(RefundGatewayConfirmationDTO confirmation, User actor) {
        RefundTransaction transaction = refundTransactionRepository.findByProviderRefundReference(confirmation.getProviderRefundReference())
                .orElseThrow(() -> new ResourceNotFoundException("Refund transaction not found"));
        RefundRequest refund = getForUpdate(transaction.getRefundRequest().getId());
        if (!confirmation.isSuccess()) {
            transaction.setStatus(RefundTransactionStatus.RETRY_REQUIRED);
            transaction.setFailureReason(confirmation.getMessage());
            refundTransactionRepository.save(transaction);
            return toResponse(refund);
        }
        transaction.setStatus(RefundTransactionStatus.SUCCEEDED);
        transaction.setConfirmedAt(LocalDateTime.now());
        transaction.setResponsePayload(confirmation.getMessage());
        refundTransactionRepository.save(transaction);
        markRefunded(refund, actor != null ? actor : refund.getCustomer(),
                actor != null && actor.getRole() == Role.ADMIN ? RefundActorType.ADMIN : RefundActorType.GATEWAY,
                "Gateway confirmed refund " + confirmation.getProviderRefundReference());
        return toResponse(refundRequestRepository.save(refund));
    }

    @Transactional
    public RefundResponseDTO retryGatewayRefund(Long refundId, User admin) {
        requireAdmin(admin);
        RefundRequest refund = getForUpdate(refundId);
        if (refund.getStatus() != RefundStatus.REFUND_PROCESSING) {
            throw new BusinessValidationException("Only processing refunds can be retried");
        }
        RefundTransaction last = refund.getTransactions().stream()
                .max(Comparator.comparing(RefundTransaction::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow(() -> new BusinessValidationException("No refund transaction exists to retry"));
        if (last.getAttemptCount() >= MAX_GATEWAY_ATTEMPTS) {
            transition(refund, RefundStatus.FAILED, admin, RefundActorType.ADMIN,
                    "Gateway retry limit reached: " + last.getFailureReason());
            return toResponse(refundRequestRepository.save(refund));
        }
        processGatewayTransaction(refund, admin, last.getAttemptCount() + 1);
        return toResponse(refundRequestRepository.save(refund));
    }

    @Transactional(readOnly = true)
    public RefundResponseDTO getRefund(Long refundId, User actor) {
        RefundRequest refund = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found"));
        assertCanView(refund, actor);
        return toResponse(refund);
    }

    @Transactional(readOnly = true)
    public Page<RefundResponseDTO> getMyRefunds(User customer, Pageable pageable) {
        return refundRequestRepository.findByCustomerAndDeletedFalseOrderByCreatedAtDesc(customer, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RefundResponseDTO> getSellerRefunds(User seller, Pageable pageable) {
        return refundRequestRepository.findBySellerAndDeletedFalseOrderByCreatedAtDesc(seller, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RefundResponseDTO> getAdminRefunds(RefundStatus status, Pageable pageable) {
        if (status != null) {
            return refundRequestRepository.findByStatusAndDeletedFalseOrderByCreatedAtDesc(status, pageable).map(this::toResponse);
        }
        return refundRequestRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public RefundAnalyticsDTO getAnalytics(int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(Math.max(1, days));
        Map<String, Long> countByReason = new LinkedHashMap<>();
        Map<String, BigDecimal> amountByReason = new LinkedHashMap<>();
        for (Object[] row : refundRequestRepository.aggregateByReason()) {
            countByReason.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
            amountByReason.put(String.valueOf(row[0]), (BigDecimal) row[2]);
        }
        return RefundAnalyticsDTO.builder()
                .totalRefundRequests(refundRequestRepository.count())
                .activeRefunds(refundRequestRepository.countActiveByStatuses(EnumSet.of(
                        RefundStatus.REQUESTED, RefundStatus.UNDER_REVIEW, RefundStatus.APPROVED, RefundStatus.REFUND_PROCESSING)))
                .refundedCount(refundRequestRepository.countActiveByStatuses(EnumSet.of(RefundStatus.REFUNDED)))
                .totalRefundedAmount(refundRequestRepository.sumTotalRefundByStatus(RefundStatus.REFUNDED))
                .countByReason(countByReason)
                .amountByReason(amountByReason)
                .dailyTrends(rowsToMaps(refundRequestRepository.trendSince(start), "date", "count", "amount"))
                .topRefundedProducts(rowsToMaps(refundRequestRepository.topRefundedProducts(PageRequest.of(0, 10)),
                        "productId", "productName", "count", "amount"))
                .sellerRefundRates(rowsToMaps(refundRequestRepository.sellerRefundSummary(PageRequest.of(0, 10)),
                        "sellerId", "sellerName", "count", "amount"))
                .categoryRefunds(rowsToMaps(refundRequestRepository.categoryRefundSummary(PageRequest.of(0, 10)),
                        "category", "count", "amount"))
                .build();
    }

    private void startRefundProcessing(RefundRequest refund, User admin) {
        transition(refund, RefundStatus.REFUND_PROCESSING, admin, RefundActorType.ADMIN,
                "Refund payment processing started");
        processGatewayTransaction(refund, admin, 1);
    }

    private void processGatewayTransaction(RefundRequest refund, User admin, int attempt) {
        RefundGateway gateway = gatewayFor(refund.getPaymentMethod());
        String idempotencyKey = "refund-" + refund.getId() + "-attempt-" + attempt;
        RefundTransaction transaction = RefundTransaction.builder()
                .refundRequest(refund)
                .gateway(gateway)
                .status(RefundTransactionStatus.PENDING)
                .amount(refund.getTotalRefund())
                .idempotencyKey(idempotencyKey)
                .providerPaymentReference(refund.getOrder().getPaymentReference())
                .requestPayload(refundGatewayService.buildRequestPayload(refund, gateway, idempotencyKey))
                .attemptCount(attempt)
                .build();
        refund.addTransaction(transaction);
        RefundGatewayService.GatewayRefundResult result = refundGatewayService.requestRefund(gateway, refund, transaction);
        transaction.setProviderRefundReference(result.getProviderRefundReference());
        transaction.setResponsePayload(result.getRawResponse());
        if (result.isSucceeded()) {
            transaction.setStatus(RefundTransactionStatus.SUCCEEDED);
            transaction.setConfirmedAt(LocalDateTime.now());
            markRefunded(refund, admin, RefundActorType.ADMIN, "Gateway returned successful refund response");
        } else if (result.isAccepted()) {
            transaction.setStatus(RefundTransactionStatus.PENDING);
            transaction.setFailureReason(result.getMessage());
        } else {
            transaction.setStatus(attempt >= MAX_GATEWAY_ATTEMPTS
                    ? RefundTransactionStatus.FAILED
                    : RefundTransactionStatus.RETRY_REQUIRED);
            transaction.setFailureReason(result.getMessage());
            if (attempt >= MAX_GATEWAY_ATTEMPTS) {
                transition(refund, RefundStatus.FAILED, admin, RefundActorType.ADMIN,
                        "Gateway refund failed after retries: " + result.getMessage());
            }
        }
    }

    private void markRefunded(RefundRequest refund, User actor, RefundActorType actorType, String note) {
        if (refund.getStatus() != RefundStatus.REFUND_PROCESSING) {
            throw new BusinessValidationException("Only processing refunds can be marked refunded");
        }
        for (RefundLineItem line : refund.getLineItems()) {
            if (line.isRestockInventory()) {
                orderStockService.restoreItemStock(line.getOrderItem(), line.getQuantityRequested());
            }
        }
        transition(refund, RefundStatus.REFUNDED, actor, actorType, note);
        refund.setRefundedAt(LocalDateTime.now());
        updateOrderPaymentAfterRefund(refund.getOrder());
        if (refund.getCustomer() != null) {
            eventPublisher.publishEvent(new LoyaltyDomainEvent(LoyaltyEventType.ORDER_REFUNDED,
                    refund.getOrder().getId(), refund.getCustomer().getId(), "refund-" + refund.getId(),
                    refund.getTotalRefund(), refund.getId()));
        }
        notificationService.createNotification(refund.getCustomer(), "Refund completed",
                "Refund request #" + refund.getId() + " has been completed.",
                NotificationType.ORDER_UPDATE, refund.getId());
    }

    private void updateOrderPaymentAfterRefund(Order order) {
        BigDecimal paidRefunds = refundRequestRepository.sumTotalRefundByOrderAndStatus(order.getId(), RefundStatus.REFUNDED);
        BigDecimal orderTotal = order.getGrandTotal() != null ? order.getGrandTotal() : BigDecimal.ZERO;
        if (paidRefunds.compareTo(orderTotal) >= 0) {
            order.setStatus(OrderStatus.REFUNDED);
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            order.setRefundPending(false);
        } else {
            order.setRefundPending(false);
            if (order.getPaymentStatus() != PaymentStatus.REFUNDED) {
                order.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
        }
        orderRepository.save(order);
    }

    private RefundLineItem calculateLineItem(Order order, OrderItem item, RefundLineItemRequestDTO request) {
        BigDecimal quantity = BigDecimal.valueOf(request.getQuantity());
        BigDecimal purchasedQuantity = BigDecimal.valueOf(item.getQuantity());
        BigDecimal itemSubtotal = item.getUnitPrice().multiply(quantity);
        BigDecimal taxRefund = proportional(item.getVatAmount(), quantity, purchasedQuantity);
        BigDecimal itemShare = order.getItemsTotal() != null && order.getItemsTotal().compareTo(BigDecimal.ZERO) > 0
                ? itemSubtotal.divide(order.getItemsTotal(), 8, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountTotal() != null
                ? order.getDiscountTotal().multiply(itemShare)
                : BigDecimal.ZERO;
        BigDecimal commission = proportional(item.getCommissionAmountSnapshot(), quantity, purchasedQuantity);
        BigDecimal total = itemSubtotal.add(taxRefund).subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
        return RefundLineItem.builder()
                .orderItem(item)
                .productIdSnapshot(item.getProductIdSnapshot())
                .productNameSnapshot(item.getProductNameSnapshot())
                .productImageSnapshot(item.getImagePathSnapshot())
                .quantityRequested(request.getQuantity())
                .unitPriceSnapshot(scale(item.getUnitPrice()))
                .itemSubtotal(scale(itemSubtotal))
                .taxRefund(scale(taxRefund))
                .discountAdjustment(scale(discount))
                .sellerCommissionReversal(scale(commission))
                .totalRefund(scale(total))
                .restockInventory(request.isRestockInventory())
                .build();
    }

    private BigDecimal calculateShippingRefund(Order order, List<RefundLineItem> lines, boolean includeShipping) {
        if (!includeShipping || order.getShippingFee() == null) {
            return BigDecimal.ZERO;
        }
        int requestedQty = lines.stream().mapToInt(RefundLineItem::getQuantityRequested).sum();
        int totalQty = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        if (requestedQty >= totalQty) {
            return order.getShippingFee();
        }
        return order.getShippingFee()
                .multiply(BigDecimal.valueOf(requestedQty))
                .divide(BigDecimal.valueOf(totalQty), 2, RoundingMode.HALF_UP);
    }

    private void applyFraudSignals(RefundRequest refund) {
        int score = 0;
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recent = refundRequestRepository.countByCustomerAndCreatedAtAfter(refund.getCustomer(), thirtyDaysAgo);
        if (recent >= 3) {
            score += 30;
            refund.addFraudSignal(signal(refund, "CUSTOMER_REFUND_VELOCITY", RefundFraudSeverity.HIGH, 30,
                    "Customer created " + recent + " refund requests in 30 days"));
        }
        long refundedRecent = refundRequestRepository.countByCustomerAndStatusAndCreatedAtAfter(
                refund.getCustomer(), RefundStatus.REFUNDED, thirtyDaysAgo);
        if (refundedRecent >= 2) {
            score += 25;
            refund.addFraudSignal(signal(refund, "REPEATED_SUCCESSFUL_REFUNDS", RefundFraudSeverity.MEDIUM, 25,
                    "Customer has " + refundedRecent + " completed refunds in 30 days"));
        }
        BigDecimal orderTotal = refund.getOrder().getGrandTotal() != null ? refund.getOrder().getGrandTotal() : BigDecimal.ZERO;
        if (orderTotal.compareTo(BigDecimal.ZERO) > 0
                && refund.getTotalRefund().compareTo(orderTotal.multiply(new BigDecimal("0.80"))) >= 0) {
            score += 20;
            refund.addFraudSignal(signal(refund, "HIGH_VALUE_REFUND", RefundFraudSeverity.MEDIUM, 20,
                    "Refund amount is at least 80% of order total"));
        }
        if (refund.getEvidence().isEmpty()) {
            score += 10;
            refund.addFraudSignal(signal(refund, "NO_EVIDENCE", RefundFraudSeverity.LOW, 10,
                    "Customer did not provide evidence"));
        }
        refund.setFraudScore(score);
        refund.setFraudFlagged(score >= 50);
    }

    private RefundFraudSignal signal(RefundRequest request, String type, RefundFraudSeverity severity, int score, String description) {
        return RefundFraudSignal.builder()
                .refundRequest(request)
                .user(request.getCustomer())
                .signalType(type)
                .severity(severity)
                .score(score)
                .description(description)
                .build();
    }

    private void attachEvidence(RefundRequest refund, User user, List<MultipartFile> files, String description) {
        if (files == null || files.isEmpty()) {
            return;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String fileName = "refund_" + refund.getId() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String path = fileStorageService.save(file, "refund_evidence", fileName);
            refund.addEvidence(RefundEvidence.builder()
                    .uploadedByUser(user)
                    .filePath(path)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .description(description)
                    .build());
        }
    }

    private void transition(RefundRequest refund, RefundStatus to, User actor, RefundActorType actorType, String note) {
        RefundStatus from = refund.getStatus();
        if (!transitionPolicy.isAllowed(from, to)) {
            throw new BusinessValidationException("Cannot transition refund from " + from + " to " + to);
        }
        refund.setStatus(to);
        refund.addHistory(history(from, to, actor, actorType, note));
    }

    private RefundStatusHistory history(RefundStatus from, RefundStatus to, User actor, RefundActorType actorType, String note) {
        return RefundStatusHistory.builder()
                .fromStatus(from)
                .toStatus(to)
                .actor(actor)
                .actorType(actorType)
                .note(note)
                .build();
    }

    private void validateCustomerOwnsOrder(User customer, Order order) {
        if (customer.getRole() == Role.ADMIN) {
            return;
        }
        if (order.getUser() == null || !order.getUser().getId().equals(customer.getId())) {
            throw new AuthorizationException("This order does not belong to you");
        }
    }

    private void validateRefundEligibility(Order order) {
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.RETURNED) {
            throw new BusinessValidationException("Refunds are only available for delivered or returned orders");
        }
        if (order.getDeliveredAt() != null && order.getDeliveredAt().plusDays(RETURN_WINDOW_DAYS).isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException("Refund window of " + RETURN_WINDOW_DAYS + " days has expired");
        }
        if (order.getPaymentStatus() != PaymentStatus.PAID
                && order.getPaymentStatus() != PaymentStatus.COD_COLLECTED
                && order.getPaymentStatus() != PaymentStatus.COD_REMITTED
                && order.getPaymentStatus() != PaymentStatus.REFUND_PENDING
                && order.getPaymentStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BusinessValidationException("Order payment status is not eligible for refund: " + order.getPaymentStatus());
        }
    }

    private User resolveSeller(List<OrderItem> items) {
        User seller = null;
        for (OrderItem item : items) {
            if (item.getProduct() == null || item.getProduct().getSellerProfile() == null
                    || item.getProduct().getSellerProfile().getUser() == null) {
                throw new BusinessValidationException("Refund item is missing seller information");
            }
            User itemSeller = item.getProduct().getSellerProfile().getUser();
            if (seller == null) {
                seller = itemSeller;
            } else if (!seller.getId().equals(itemSeller.getId())) {
                throw new BusinessValidationException("A refund request can only contain items from one seller order");
            }
        }
        return seller;
    }

    private RefundRequest getForUpdate(Long refundId) {
        return refundRequestRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found"));
    }

    private RefundGateway gatewayFor(PaymentMethod method) {
        if (method == PaymentMethod.ESEWA) return RefundGateway.ESEWA;
        if (method == PaymentMethod.KHALTI) return RefundGateway.KHALTI;
        if (method == PaymentMethod.COD) return RefundGateway.COD_MANUAL;
        return RefundGateway.MANUAL;
    }

    private BigDecimal proportional(BigDecimal amount, BigDecimal numerator, BigDecimal denominator) {
        if (amount == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(numerator).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeIdempotencyKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 120) {
            throw new BusinessValidationException("Idempotency key is too long");
        }
        return trimmed;
    }

    private void assertCanView(RefundRequest refund, User actor) {
        if (actor.getRole() == Role.ADMIN
                || refund.getCustomer().getId().equals(actor.getId())
                || refund.getSeller().getId().equals(actor.getId())) {
            return;
        }
        throw new AuthorizationException("You do not have permission to view this refund");
    }

    private void requireAdmin(User actor) {
        if (actor.getRole() != Role.ADMIN) {
            throw new AuthorizationException("Admin access is required");
        }
    }

    private List<Map<String, Object>> rowsToMaps(List<Object[]> rows, String... keys) {
        return rows.stream().map(row -> {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < keys.length && i < row.length; i++) {
                map.put(keys[i], row[i]);
            }
            return map;
        }).collect(Collectors.toList());
    }

    private RefundResponseDTO toResponse(RefundRequest refund) {
        return RefundResponseDTO.builder()
                .id(refund.getId())
                .orderId(refund.getOrder().getId())
                .customerId(refund.getCustomer().getId())
                .customerName(refund.getCustomer().getFullName())
                .sellerId(refund.getSeller().getId())
                .sellerName(refund.getSeller().getFullName())
                .reason(refund.getReason())
                .reasonDetails(refund.getReasonDetails())
                .status(refund.getStatus())
                .paymentMethod(refund.getPaymentMethod())
                .itemSubtotal(refund.getItemSubtotal())
                .taxRefund(refund.getTaxRefund())
                .shippingRefund(refund.getShippingRefund())
                .discountAdjustment(refund.getDiscountAdjustment())
                .totalRefund(refund.getTotalRefund())
                .sellerCommissionReversal(refund.getSellerCommissionReversal())
                .shippingIncluded(refund.isShippingIncluded())
                .fraudScore(refund.getFraudScore())
                .fraudFlagged(refund.isFraudFlagged())
                .customerNotes(refund.getCustomerNotes())
                .sellerNotes(refund.getSellerNotes())
                .adminNotes(refund.getAdminNotes())
                .createdAt(refund.getCreatedAt())
                .updatedAt(refund.getUpdatedAt())
                .approvedAt(refund.getApprovedAt())
                .refundedAt(refund.getRefundedAt())
                .cancelledAt(refund.getCancelledAt())
                .items(refund.getLineItems().stream().map(this::lineToResponse).toList())
                .evidence(refund.getEvidence().stream().map(this::evidenceToResponse).toList())
                .timeline(refund.getStatusHistory().stream()
                        .sorted(Comparator.comparing(RefundStatusHistory::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::historyToResponse).toList())
                .transactions(refund.getTransactions().stream().map(this::transactionToResponse).toList())
                .build();
    }

    private RefundLineItemResponseDTO lineToResponse(RefundLineItem line) {
        return RefundLineItemResponseDTO.builder()
                .id(line.getId())
                .orderItemId(line.getOrderItem().getId())
                .productId(line.getProductIdSnapshot())
                .productName(line.getProductNameSnapshot())
                .productImage(line.getProductImageSnapshot())
                .quantityRequested(line.getQuantityRequested())
                .itemSubtotal(line.getItemSubtotal())
                .taxRefund(line.getTaxRefund())
                .discountAdjustment(line.getDiscountAdjustment())
                .totalRefund(line.getTotalRefund())
                .sellerCommissionReversal(line.getSellerCommissionReversal())
                .restockInventory(line.isRestockInventory())
                .build();
    }

    private RefundEvidenceDTO evidenceToResponse(RefundEvidence evidence) {
        return RefundEvidenceDTO.builder()
                .id(evidence.getId())
                .filePath(evidence.getFilePath())
                .fileType(evidence.getFileType())
                .fileSize(evidence.getFileSize())
                .description(evidence.getDescription())
                .uploadedByUserId(evidence.getUploadedByUser().getId())
                .uploadedByName(evidence.getUploadedByUser().getFullName())
                .createdAt(evidence.getCreatedAt())
                .build();
    }

    private RefundTimelineDTO historyToResponse(RefundStatusHistory history) {
        return RefundTimelineDTO.builder()
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .actorType(history.getActorType())
                .actorId(history.getActor() != null ? history.getActor().getId() : null)
                .actorName(history.getActor() != null ? history.getActor().getFullName() : history.getActorType().name())
                .note(history.getNote())
                .createdAt(history.getCreatedAt())
                .build();
    }

    private RefundTransactionDTO transactionToResponse(RefundTransaction transaction) {
        return RefundTransactionDTO.builder()
                .id(transaction.getId())
                .gateway(transaction.getGateway())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .providerRefundReference(transaction.getProviderRefundReference())
                .failureReason(transaction.getFailureReason())
                .attemptCount(transaction.getAttemptCount())
                .createdAt(transaction.getCreatedAt())
                .confirmedAt(transaction.getConfirmedAt())
                .build();
    }
}
