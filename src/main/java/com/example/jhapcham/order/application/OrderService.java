package com.example.jhapcham.order.application;


import com.example.jhapcham.order.application.*;
import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.order.dto.*;
import com.example.jhapcham.order.persistence.*;
import com.example.jhapcham.cart.domain.CartItem;
import com.example.jhapcham.cart.persistence.CartItemRepository;
import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.activity.domain.ActivityType;
import com.example.jhapcham.activity.application.UserActivityService;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.product.domain.Product;
import com.example.jhapcham.product.persistence.ProductRepository;
import com.example.jhapcham.product.domain.ProductStatus;
import com.example.jhapcham.product.domain.ProductVariant;
import com.example.jhapcham.product.persistence.ProductVariantRepository;
import com.example.jhapcham.seller.domain.SellerProfile;
import com.example.jhapcham.seller.persistence.SellerProfileRepository;
import com.example.jhapcham.loyalty.domain.LoyaltyDomainEvent;
import com.example.jhapcham.loyalty.domain.LoyaltyEventType;
import com.example.jhapcham.loyalty.dto.RedemptionQuoteDTO;
import com.example.jhapcham.payment.domain.Payment;
import com.example.jhapcham.payment.persistence.PaymentRepository;
import com.example.jhapcham.payment.domain.PaymentState;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.user.domain.User;
import com.example.jhapcham.user.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.example.jhapcham.promocode.application.PromoCodeService;
import com.example.jhapcham.promocode.application.PromoCodeService.DiscountResult;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private static final SecureRandom DELIVERY_OTP_RANDOM = new SecureRandom();
    private static final int DELIVERY_OTP_EXPIRY_MINUTES = 10;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserActivityService userActivityService;
    private final PaymentRepository paymentRepository;

    // Injected specialized services
    private final OrderStockService orderStockService;
    private final OrderAccountingService orderAccountingService;
    private final OrderStatusService orderStatusService;
    private final com.example.jhapcham.notification.application.NotificationService notificationService;
    private final com.example.jhapcham.notification.application.EmailService emailService;
    private final com.example.jhapcham.promocode.application.PromoCodeService promoCodeService;
    private final com.example.jhapcham.loyalty.application.LoyaltyService loyaltyService;
    private final ObjectProvider<com.example.jhapcham.delivery.application.ShipmentService> shipmentServiceProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderIdGenerator orderIdGenerator;

    // =========================
    // PREVIEW ORDER
    // =========================
    @Transactional
    public OrderPreviewDTO previewOrder(CheckoutRequestDTO dto) {
        validateItems(dto);
        validateCouponUser(dto.getCouponCode(), dto.getUserId());

        // Group items by Seller to simulate the split
        Map<Long, List<CheckoutItemDTO>> itemsBySeller = groupItemsBySeller(dto.getItems());

        BigDecimal totalItemsCost = BigDecimal.ZERO;
        BigDecimal totalShipping = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalGrand = BigDecimal.ZERO;
        List<OrderItemResponseDTO> allItemResponses = new ArrayList<>();

        for (List<CheckoutItemDTO> sellerItems : itemsBySeller.values()) {
            CheckoutComputationResult data = computeForItems(sellerItems, dto.getShippingLocation(), null, null);

            totalItemsCost = totalItemsCost.add(data.itemsTotal);
            totalShipping = totalShipping.add(data.shippingFee);
            totalVat = totalVat.add(data.vatAmount);
            totalGrand = totalGrand.add(data.grandTotal);
            allItemResponses.addAll(data.itemResponses);
        }

        BigDecimal totalDiscount = BigDecimal.ZERO;
        if (dto.getCouponCode() != null && !dto.getCouponCode().isBlank()) {
            totalDiscount = promoCodeService.calculateDiscount(dto.getCouponCode(), dto.getItems(), dto.getUserId()).getAmount();
            totalGrand = totalGrand.subtract(totalDiscount);
            if (totalGrand.compareTo(BigDecimal.ZERO) < 0) {
                totalGrand = BigDecimal.ZERO;
            }
        }

        return OrderPreviewDTO.builder()
                .items(allItemResponses)
                .itemsTotal(totalItemsCost)
                .shippingFee(totalShipping)
                .vatAmount(totalVat)
                .discountTotal(totalDiscount)
                .loyaltyDiscountAmount(BigDecimal.ZERO)
                .loyaltyPointsRedeemed(0L)
                .grandTotal(totalGrand)
                .estimatedDelivery(estimateDelivery(dto.getShippingLocation()))
                .build();
    }

    // =========================
    // PLACE ORDER
    // =========================
    @Transactional
    public List<OrderSummaryDTO> placeOrder(CheckoutRequestDTO dto) {
        validateCustomerFields(dto);
        validateItems(dto);

        User user = dto.getUserId() != null
                ? userRepository.findById(dto.getUserId()).orElse(null)
                : null;
        validateCouponUser(dto.getCouponCode(), user != null ? user.getId() : null);
        String idempotencyKey = normalizeIdempotencyKey(dto.getIdempotencyKey());
        List<Order> existingOrders = findExistingIdempotentOrders(user, dto.getEmail(), idempotencyKey);
        if (!existingOrders.isEmpty()) {
            return existingOrders.stream()
                    .map(order -> toSummaryDTO(order, mapItems(order)))
                    .toList();
        }

        PaymentMethod paymentMethod = parsePaymentMethod(dto.getPaymentMethod());

        // Group items by Seller to split orders
        Map<Long, List<CheckoutItemDTO>> itemsBySeller = groupItemsBySeller(dto.getItems());

        List<OrderSummaryDTO> summaries = new ArrayList<>();
        Map<Long, CheckoutComputationResult> sellerComputations = new LinkedHashMap<>();
        BigDecimal totalItemsSum = BigDecimal.ZERO;
        BigDecimal groupedGrandBeforeDiscount = BigDecimal.ZERO;
        for (Map.Entry<Long, List<CheckoutItemDTO>> entry : itemsBySeller.entrySet()) {
            CheckoutComputationResult sellerData = computeForItems(entry.getValue(), dto.getShippingLocation(), null, null);
            sellerComputations.put(entry.getKey(), sellerData);
            totalItemsSum = totalItemsSum.add(sellerData.itemsTotal);
            groupedGrandBeforeDiscount = groupedGrandBeforeDiscount.add(sellerData.grandTotal);
        }

        DiscountResult discountResult = promoCodeService.calculateDiscount(dto.getCouponCode(), dto.getItems(),
                user != null ? user.getId() : null);
        BigDecimal globalDiscount = discountResult.getAmount();
        boolean anyPromoApplied = globalDiscount.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal groupedGrandAfterDiscount = groupedGrandBeforeDiscount.subtract(globalDiscount);
        if (groupedGrandAfterDiscount.compareTo(BigDecimal.ZERO) < 0) {
            groupedGrandAfterDiscount = BigDecimal.ZERO;
        }

        BigDecimal loyaltyDiscountBudget = BigDecimal.ZERO;
        Long loyaltyPointsBudget = 0L;
        if (user != null && dto.getLoyaltyPointsToRedeem() != null && dto.getLoyaltyPointsToRedeem() > 0
                && groupedGrandAfterDiscount.compareTo(BigDecimal.ZERO) > 0) {
            RedemptionQuoteDTO quote = loyaltyService.quoteRedemption(user.getId(), groupedGrandAfterDiscount, dto.getLoyaltyPointsToRedeem());
            loyaltyDiscountBudget = quote.getDiscountAmount();
            loyaltyPointsBudget = quote.getApprovedPoints();
        }
        BigDecimal remainingLoyaltyDiscount = loyaltyDiscountBudget;
        Long remainingLoyaltyPoints = loyaltyPointsBudget;
        int sellerIndex = 0;

        // 2. Iterate through each seller's group and create sub-orders
        for (Map.Entry<Long, List<CheckoutItemDTO>> entry : itemsBySeller.entrySet()) {
            sellerIndex++;

            // Calculate base values for this specific seller's order
            CheckoutComputationResult sellerBaseData = sellerComputations.get(entry.getKey());

            // Smart discount distribution: SELLER_ONLY vs GLOBAL
            BigDecimal sellerDiscount = BigDecimal.ZERO;
            BigDecimal sellerPromoDiscount = BigDecimal.ZERO;
            BigDecimal platformSponsoredDiscount = BigDecimal.ZERO;

            if (discountResult.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (discountResult.getApplicableSellerId() != null) {
                    // This is a SELLER_ONLY coupon. Only apply to the owner seller's sub-order.
                    if (discountResult.getApplicableSellerId().equals(entry.getKey())) {
                        sellerDiscount = discountResult.getAmount();
                        sellerPromoDiscount = sellerDiscount;
                    }
                } else {
                    // This is a GLOBAL coupon. Distribute proportionally across all sellers.
                    if (totalItemsSum.compareTo(BigDecimal.ZERO) > 0) {
                        sellerDiscount = discountResult.getAmount().multiply(sellerBaseData.itemsTotal)
                                .divide(totalItemsSum, 2, RoundingMode.HALF_UP);
                        platformSponsoredDiscount = sellerDiscount;
                    }
                }
            }

            // Recalculate Grand Total for this seller: Items + Shipping + VAT - Distributed Discount
            BigDecimal sellerGrandTotal = sellerBaseData.itemsTotal
                    .add(sellerBaseData.shippingFee)
                    .subtract(sellerDiscount);

            if (sellerGrandTotal.compareTo(BigDecimal.ZERO) < 0) sellerGrandTotal = BigDecimal.ZERO;

            BigDecimal sellerLoyaltyDiscount = BigDecimal.ZERO;
            Long sellerLoyaltyPoints = 0L;
            if (loyaltyPointsBudget > 0 && groupedGrandAfterDiscount.compareTo(BigDecimal.ZERO) > 0) {
                if (sellerIndex == itemsBySeller.size()) {
                    sellerLoyaltyDiscount = remainingLoyaltyDiscount;
                    sellerLoyaltyPoints = remainingLoyaltyPoints;
                } else {
                    BigDecimal share = sellerGrandTotal.divide(groupedGrandAfterDiscount, 8, RoundingMode.HALF_UP);
                    sellerLoyaltyDiscount = loyaltyDiscountBudget.multiply(share).setScale(2, RoundingMode.HALF_UP);
                    sellerLoyaltyPoints = Math.min(remainingLoyaltyPoints,
                            sellerLoyaltyDiscount.setScale(0, RoundingMode.DOWN).longValue());
                }
                if (sellerLoyaltyDiscount.compareTo(sellerGrandTotal) > 0) {
                    sellerLoyaltyDiscount = sellerGrandTotal;
                }
                remainingLoyaltyDiscount = remainingLoyaltyDiscount.subtract(sellerLoyaltyDiscount);
                remainingLoyaltyPoints = Math.max(0L, remainingLoyaltyPoints - sellerLoyaltyPoints);
                sellerGrandTotal = sellerGrandTotal.subtract(sellerLoyaltyDiscount);
            }

            Order order = Order.builder()
                    .user(user)
                    .customerName(dto.getFullName())
                    .customerPhone(dto.getPhone())
                    .customerEmail(dto.getEmail())
                    .shippingAddress(dto.getAddress())
                    .shippingLocation(dto.getShippingLocation())
                    .customerAlternativePhone(dto.getAlternativePhone())
                    .deliveryTimePreference(dto.getDeliveryTimePreference())
                    .orderNote(dto.getOrderNote())
                    .paymentMethod(paymentMethod)
                    .status(paymentMethod == PaymentMethod.COD ? OrderStatus.COD_PENDING : OrderStatus.DRAFT)
                    .paymentStatus(initialPaymentStatus(paymentMethod))
                    .itemsTotal(sellerBaseData.itemsTotal)
                    .shippingFee(sellerBaseData.shippingFee)
                    .vatAmount(sellerBaseData.vatAmount)
                    .discountTotal(sellerDiscount) // DISTRIBUTED DISCOUNT
                    .sellerPromoDiscountAmount(sellerPromoDiscount)
                    .platformSponsoredDiscountAmount(platformSponsoredDiscount)
                    .loyaltyDiscountAmount(sellerLoyaltyDiscount)
                    .loyaltyPointsRedeemed(sellerLoyaltyPoints)
                    .grandTotal(sellerGrandTotal)
                    .createdAt(LocalDateTime.now())
                    .appliedCoupon(dto.getCouponCode())
                    .idempotencyKey(buildOrderIdempotencyKey(idempotencyKey, entry.getKey()))
                    // Initialize accounting fields to ZERO
                    .sellerGrossAmount(BigDecimal.ZERO)
                    .sellerShippingCharge(BigDecimal.ZERO)
                    .sellerNetAmount(BigDecimal.ZERO)
                    .sellerAccounted(false)
                    .build();

            // ---------------------------------------------------------------
            // Generate human-readable order ID: JHC-YYYYMMDD-XXXX
            // Done before the first save so the field is persisted atomically.
            // ---------------------------------------------------------------
            String customOrderId = orderIdGenerator.generate(LocalDate.now());
            order.setCustomOrderId(customOrderId);

            orderRepository.save(order);

            if (user != null && sellerLoyaltyPoints > 0) {
                BigDecimal beforeLoyalty = sellerGrandTotal.add(sellerLoyaltyDiscount);
                RedemptionQuoteDTO applied = loyaltyService.commitCheckoutRedemption(user, order, sellerLoyaltyPoints, beforeLoyalty);
                order.setLoyaltyPointsRedeemed(applied.getApprovedPoints());
                order.setLoyaltyDiscountAmount(applied.getDiscountAmount());
                order.setGrandTotal(beforeLoyalty.subtract(applied.getDiscountAmount()));
                orderRepository.save(order);
            }

            LocalDateTime paymentNow = LocalDateTime.now();
            Payment payment = Payment.builder()
                    .order(order)
                    .method(paymentMethod)
                    .state(paymentMethod == PaymentMethod.COD ? PaymentState.PENDING : PaymentState.INITIATED)
                    .amount(order.getGrandTotal())
                    .initiatedAt(paymentMethod == PaymentMethod.COD ? null : paymentNow)
                    .createdAt(paymentNow)
                    .updatedAt(paymentNow)
                    .build();
            paymentRepository.save(payment);

            for (OrderItemResponseDTO r : sellerBaseData.itemResponses) {
                Product product = sellerBaseData.productById.get(r.getProductId());

                // Resolve variant
                ProductVariant variant = r.getVariantId() != null
                        ? productVariantRepository.findById(r.getVariantId()).orElse(null)
                        : null;

                BigDecimal itemVat = calculateIncludedVat(r.getLineTotal());
                BigDecimal buyingUnitPrice = product.getBuyingPrice() != null ? product.getBuyingPrice() : BigDecimal.ZERO;
                BigDecimal buyingLineTotal = buyingUnitPrice.multiply(BigDecimal.valueOf(r.getQuantity()))
                        .setScale(2, RoundingMode.HALF_UP);

                // Build JSON snapshot of variant attributes for order history
                String attrSnapshot = buildAttrSnapshot(r.getVariantAttributes());

                OrderItem item = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .variant(variant)
                        .productIdSnapshot(product.getId())
                        .productNameSnapshot(product.getName())
                        .brandSnapshot(product.getBrand())
                        .imagePathSnapshot(r.getImagePath())
                        .quantity(r.getQuantity())
                        .unitPrice(r.getUnitPrice())
                        .vatAmount(itemVat)
                        .lineTotal(r.getLineTotal())
                        .buyingUnitPriceSnapshot(buyingUnitPrice)
                        .buyingLineTotalSnapshot(buyingLineTotal)
                        .sellingLineTotalSnapshot(r.getLineTotal())
                        .variantSkuSnapshot(variant != null ? variant.getSku() : null)
                        .variantAttributesSnapshot(attrSnapshot)
                        .manufactureDateSnapshot(product.getManufactureDate())
                        .expiryDateSnapshot(product.getExpiryDate())
                        .productDescriptionSnapshot(product.getDescription())
                        .specificationSnapshot(product.getSpecification())
                        .featuresSnapshot(product.getFeatures())
                        .build();

                orderItemRepository.save(item);
                order.addItem(item);

                if (user != null) {
                    userActivityService.recordActivity(user.getId(), product.getId(), ActivityType.ORDER,
                            "Bought " + r.getQuantity() + " item(s)");
                }
            }

            if (paymentMethod == PaymentMethod.COD) {
                orderStockService.deductStockForOrder(order);
            }
            
            // Now that all items are added, initialize accounting
            orderAccountingService.initializeAccounting(order);

            orderRepository.save(order);
            summaries.add(toSummaryDTO(order, mapItems(order)));
            log.info("Order {} [{}] placed successfully for customer {}",
                    order.getCustomOrderId(), order.getId(), dto.getFullName());

            sendPlacementNotifications(order, user);
        }

        // Increment promo usage if it was applied effectively to at least one sub-order
        if (anyPromoApplied && dto.getCouponCode() != null && !dto.getCouponCode().isEmpty()) {
            promoCodeService.incrementUsage(dto.getCouponCode(), user != null ? user.getId() : null);
        }

        return summaries;
    }

    @Transactional
    public List<OrderSummaryDTO> placeOrderFromCart(CartCheckoutRequestDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new BusinessValidationException("Cart is empty");
        }

        List<CheckoutItemDTO> checkoutItems = new ArrayList<>();
        for (CartItem c : cartItems) {
            CheckoutItemDTO it = new CheckoutItemDTO();
            it.setProductId(c.getProduct().getId());
            it.setVariantId(c.getVariant() != null ? c.getVariant().getId() : null);
            it.setQuantity(c.getQuantity());
            checkoutItems.add(it);
        }

        CheckoutRequestDTO checkout = new CheckoutRequestDTO();
        checkout.setUserId(user.getId());
        checkout.setFullName(dto.getFullName());
        checkout.setPhone(dto.getPhone());
        checkout.setEmail(dto.getEmail());
        checkout.setAddress(dto.getAddress());
        checkout.setAlternativePhone(dto.getAlternativePhone());
        checkout.setDeliveryTimePreference(dto.getDeliveryTimePreference());
        checkout.setOrderNote(dto.getOrderNote());
        checkout.setShippingLocation(dto.getShippingLocation());
        checkout.setPaymentMethod(dto.getPaymentMethod());
        checkout.setCouponCode(dto.getCouponCode());
        checkout.setIdempotencyKey(dto.getIdempotencyKey());
        checkout.setLoyaltyPointsToRedeem(dto.getLoyaltyPointsToRedeem());
        checkout.setItems(checkoutItems);

        List<OrderSummaryDTO> summaries = placeOrder(checkout);
        cartItemRepository.deleteAll(cartItems);

        return summaries;
    }

    // =========================
    // SELLER / BRANCH / CANCEL
    // =========================

    @Transactional
    public OrderSummaryDTO sellerProcessOrder(Long orderId, Long sellerId) {
        return updateSellerOwnedOrderStatus(orderId, sellerId, OrderStatus.PROCESSING, "Seller moved order to processing");
    }

    @Transactional
    public OrderSummaryDTO sellerAssignBranch(Long orderId, Long sellerId, AssignBranchDTO dto) {
        Order order = getOrderOrFail(orderId);
        if (!sellerOwns(order, sellerId)) {
            throw new AuthorizationException("You do not have permission to assign branches to this order");
        }

        order.setAssignedBranch(DeliveryBranch.fromString(dto.getBranch()));
        return applyOrderStatusChange(order, OrderStatus.PACKED,
                "Seller assigned branch " + dto.getBranch() + " and prepared shipment");
    }

    @Transactional
    public OrderSummaryDTO branchUpdateStatus(Long orderId, String branchRaw, String nextStatusRaw) {
        Order order = getOrderOrFail(orderId);
        DeliveryBranch branch = DeliveryBranch.fromString(branchRaw);
        OrderStatus next = OrderStatus.valueOf(nextStatusRaw.toUpperCase());

        if (order.getAssignedBranch() == null) {
            throw new BusinessValidationException("Branch not assigned to this order");
        }
        if (order.getAssignedBranch() != branch) {
            throw new BusinessValidationException("Branch mismatch for this order");
        }

        return applyOrderStatusChange(order, next,
                "Branch " + branchRaw + " updated order to " + nextStatusRaw);
    }

    @Transactional
    public OrderSummaryDTO verifyDeliveryOtp(Long orderId, String branchRaw, String otp) {
        Order order = getOrderOrFail(orderId);
        DeliveryBranch branch = DeliveryBranch.fromString(branchRaw);

        if (order.getAssignedBranch() == null || order.getAssignedBranch() != branch) {
            throw new BusinessValidationException("Branch mismatch for this order");
        }

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessValidationException("Order is not in shipped state");
        }

        if (order.getDeliveryOtp() == null || !order.getDeliveryOtp().equals(otp)) {
            throw new BusinessValidationException("Invalid OTP provided");
        }

        if (LocalDateTime.now().isAfter(order.getDeliveryOtpExpiry())) {
            throw new BusinessValidationException("OTP has expired");
        }

        // OTP verified successfully
        order.setDeliveredBranch(branch);
        applyOrderStatusChange(order, OrderStatus.DELIVERED,
                "Delivery OTP verified successfully");
        orderRepository.save(order);
        log.info("Order {} [{}] delivered successfully with OTP verification",
                order.getCustomOrderId(), orderId);

        // Send Final Status Update Email
        emailService.sendOrderStatusUpdateEmail(
                order.getCustomerEmail(), order.getCustomerName(),
                order.getCustomOrderId(), "DELIVERED");

        try {
            notificationService.createNotification(
                    order.getUser(),
                    "Order Delivered",
                    "Your order " + order.getCustomOrderId() + " has been successfully delivered.",
                    com.example.jhapcham.notification.domain.NotificationType.ORDER_UPDATE,
                    order.getId());
        } catch (Exception e) {
            log.error("Failed to notify customer of delivery", e);
        }

        return toSummaryDTO(order, mapItems(order));
    }

    @Transactional
    public void resendDeliveryOtp(Long orderId, String branchRaw) {
        Order order = getOrderOrFail(orderId);
        DeliveryBranch branch = DeliveryBranch.fromString(branchRaw);

        if (order.getAssignedBranch() == null || order.getAssignedBranch() != branch) {
            throw new BusinessValidationException("Branch mismatch for this order");
        }

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessValidationException("Order is not in shipped state");
        }

        if (order.getDeliveryOtpResendCount() >= 5) {
            throw new BusinessValidationException("Maximum OTP resend attempts exceeded");
        }

        issueDeliveryOtp(order);
        order.setDeliveryOtpResendCount(order.getDeliveryOtpResendCount() + 1);

        emailService.sendDeliveryOtpEmail(order.getCustomerEmail(), order.getCustomerName(), order.getDeliveryOtp());
        orderRepository.save(order);
        log.info("Resent delivery OTP for order {}", orderId);
    }


    @Transactional
    public OrderSummaryDTO sellerExpressDispatch(Long orderId, Long sellerId, AssignBranchDTO dto) {
        Order order = getOrderOrFail(orderId);
        if (!sellerOwns(order, sellerId)) {
            throw new AuthorizationException("You do not have permission to dispatch this order");
        }

        DeliveryBranch branch = DeliveryBranch.fromString(dto.getBranch());
        order.setAssignedBranch(branch);

        issueDeliveryOtp(order);
        
        emailService.sendDeliveryOtpEmail(order.getCustomerEmail(), order.getCustomerName(), order.getDeliveryOtp());
        
        return applyOrderStatusChange(order, OrderStatus.SHIPPED,
                "Seller performed express dispatch through branch " + branch);
    }

    @Transactional
    public OrderSummaryDTO sellerCancelOrder(Long orderId, Long sellerId, String reason) {
        Order order = getOrderOrFail(orderId);
        if (!sellerOwns(order, sellerId)) {
            throw new AuthorizationException("You do not have permission to cancel this order");
        }

        if (!orderStatusService.canCancel(order.getStatus())) {
            throw new com.example.jhapcham.Error.OrderStateConflictException(
                    "Cannot cancel order in status: " + order.getStatus());
        }

        // Save cancellation reason to orderNote so it shows up on customer order page
        if (reason != null && !reason.trim().isEmpty()) {
            order.setOrderNote("Merchant Cancellation Reason: " + reason);
        }

        OrderSummaryDTO summary = applyOrderStatusChange(order, OrderStatus.CANCELLED,
                "Shipment cancelled after merchant cancellation: " + (reason != null ? reason : ""));

        emailService.sendOrderStatusUpdateEmail(
                order.getCustomerEmail(), order.getCustomerName(),
                order.getCustomOrderId(), "CANCELED_BY_MERCHANT");

        try {
            String msg = "We regret to inform you that your order " + order.getCustomOrderId() + " was canceled by the merchant.";
            if (reason != null && !reason.trim().isEmpty()) {
                msg += " Reason: " + reason;
            }
            notificationService.createNotification(
                    order.getUser(),
                    "Order Canceled by Merchant",
                    msg,
                    com.example.jhapcham.notification.domain.NotificationType.ORDER_UPDATE,
                    order.getId());
        } catch (Exception e) {
            log.error("Failed to notify customer of order cancellation", e);
        }

        return summary;
    }

    @Transactional
    public OrderSummaryDTO customerCancelOrder(Long orderId, Long userId) {
        Order order = getOrderOrFail(orderId);
        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new AuthorizationException("You do not have permission to cancel this order");
        }

        if (!orderStatusService.canCancel(order.getStatus())) {
            throw new com.example.jhapcham.Error.OrderStateConflictException(
                    "Order cannot be canceled now (Status: " + order.getStatus() + ")");
        }

        return applyOrderStatusChange(order, OrderStatus.CANCELLED,
                "Shipment cancelled after customer cancellation");
    }

    @Transactional
    public OrderSummaryDTO retryPayment(Long orderId, User actor) {
        Order order = getOrderOrFail(orderId);
        assertOrderAccess(order, actor);

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessValidationException("Only draft orders can be retried for payment");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessValidationException("Order is already paid");
        }
        if (order.getPaymentMethod() != PaymentMethod.ESEWA && order.getPaymentMethod() != PaymentMethod.KHALTI) {
            throw new BusinessValidationException("Retry is only supported for eSewa and Khalti payments");
        }

        order.setPaymentStatus(PaymentStatus.PAYMENT_INITIATED);
        order.setPaymentInitiatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return toSummaryDTO(order, mapItems(order));
    }

    @Transactional
    public OrderSummaryDTO changePaymentMethod(Long orderId, String newMethod, User actor) {
        Order order = getOrderOrFail(orderId);
        assertOrderAccess(order, actor);

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessValidationException("Payment method can only be changed on draft orders");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessValidationException("Order is already paid");
        }

        PaymentMethod method = parsePaymentMethod(newMethod);
        if (method != PaymentMethod.ESEWA && method != PaymentMethod.KHALTI) {
            throw new BusinessValidationException("Only eSewa and Khalti payment methods are supported");
        }

        order.setPaymentMethod(method);
        order.setPaymentStatus(PaymentStatus.PAYMENT_INITIATED);
        order.setPaymentInitiatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Also update the linked Payment record's method if it exists
        paymentRepository.findByOrder(order).ifPresent(p -> {
            p.setMethod(method);
            p.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(p);
        });

        return toSummaryDTO(order, mapItems(order));
    }

    @Transactional
    public OrderSummaryDTO guestCancelOrder(Long orderId) {
        throw new AuthorizationException("Guest order cancellation requires a secure verification flow");
    }

    // =========================
    // FETCH
    // =========================
    @Transactional(readOnly = true)
    public OrderSummaryDTO getOrder(Long orderId, User actor) {
        Order order = getOrderOrFail(orderId);
        assertOrderAccess(order, actor);
        return toSummaryDTO(order, mapItems(order));
    }

    /**
     * Fetches an order by its human-readable reference (e.g. {@code JHC-20260520-0003}).
     * Applies the same access-control rules as {@link #getOrder(Long, User)}.
     *
     * @param customOrderId e.g. {@code "JHC-20260520-0003"}
     * @param actor         the authenticated user requesting the order
     */
    @Transactional(readOnly = true)
    public OrderSummaryDTO getOrderByRef(String customOrderId, User actor) {
        Order order = orderRepository.findByCustomOrderId(customOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + customOrderId));
        assertOrderAccess(order, actor);
        return toSummaryDTO(order, mapItems(order));
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> getOrdersForUser(Long userId) {
        // Legacy safety: some orders were placed without linking User, but with customerEmail.
        User user = userRepository.findById(userId).orElse(null);
        String email = user != null ? user.getEmail() : null;
        List<Order> orders = (email != null)
                ? orderRepository.findForUserOrEmail(userId, email)
                : orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream().map(o -> toSummaryDTO(o, mapItems(o))).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderListItemDTO> getOrdersForUserSimple(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        String email = user != null ? user.getEmail() : null;
        List<Order> orders = (email != null)
                ? orderRepository.findForUserOrEmail(userId, email)
                : orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream().map(o -> {
            String productNames = o.getItems().stream()
                    .map(OrderItem::getProductNameSnapshot)
                    .collect(Collectors.joining(", "));
            
            OrderItem firstItem = o.getItems().isEmpty() ? null : o.getItems().get(0);
            String image = resolveProductImage(firstItem);

            return OrderListItemDTO.builder()
                    .orderId(o.getId())
                    .customOrderId(o.getCustomOrderId())
                    .status(o.getStatus())
                    .grandTotal(o.getGrandTotal())
                    .itemsTotal(o.getItemsTotal())
                    .paymentMethod(o.getPaymentMethod())
                    .paymentStatus(o.getPaymentStatus())
                    .paymentReference(o.getPaymentReference())
                    .deliveryStatus(o.getShipment() != null ? o.getShipment().getStatus() : null)
                    .discountTotal(o.getDiscountTotal())
                    .totalItems(o.getItems().size())
                    .createdAt(o.getCreatedAt())
                    .appliedCoupon(o.getAppliedCoupon())
                    .productNames(productNames)
                    .productImage(image)
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderListItemDTO> getOrdersForSeller(Long sellerUserId) {
        List<Order> orders = orderRepository.findOrdersBySeller(sellerUserId);

        return orders.stream().map(o -> {
            List<OrderItem> sellerItems = o.getItems().stream()
                    .filter(i -> i.getProduct() != null &&
                            i.getProduct().getSellerProfile() != null &&
                            i.getProduct().getSellerProfile().getUser() != null &&
                            i.getProduct().getSellerProfile().getUser().getId().equals(sellerUserId))
                    .collect(Collectors.toList());

            OrderItem firstItem = sellerItems.isEmpty() ? (!o.getItems().isEmpty() ? o.getItems().get(0) : null)
                    : sellerItems.get(0);

            String image = resolveProductImage(firstItem);

            return OrderListItemDTO.builder()
                    .orderId(o.getId())
                    .customOrderId(o.getCustomOrderId())
                    .status(o.getStatus())
                    .grandTotal(o.getGrandTotal())
                    .itemsTotal(o.getItemsTotal())
                    .discountTotal(o.getDiscountTotal())
                    .totalItems(o.getItems().size())
                    .sellerGrossAmount(o.getSellerGrossAmount())
                    .sellerShippingCharge(o.getSellerShippingCharge())
                    .vatAmount(o.getVatAmount())
                    .marketplaceCommission(o.getMarketplaceCommission())
                    .sellerNetAmount(o.getSellerNetAmount())
                    .deliveredBranch(o.getDeliveredBranch())
                    .assignedBranch(o.getAssignedBranch())
                    .paymentMethod(o.getPaymentMethod())
                    .paymentStatus(o.getPaymentStatus())
                    .paymentReference(o.getPaymentReference())
                    .deliveryStatus(o.getShipment() != null ? o.getShipment().getStatus() : null)
                    .createdAt(o.getCreatedAt())
                    .appliedCoupon(o.getAppliedCoupon())
                    .customerName(o.getCustomerName())
                    .customerPhone(o.getCustomerPhone())
                    .customerId(o.getUser() != null ? o.getUser().getId() : null)
                    .orderNote(o.getOrderNote())
                    .deliveryTimePreference(o.getDeliveryTimePreference())
                    .productNames(sellerItems.stream()
                            .map(OrderItem::getProductNameSnapshot)
                            .collect(Collectors.joining(", ")))
                    .productImage(image)
                    .customerProfileImagePath(o.getUser() != null ? o.getUser().getProfileImagePath() : null)
                    .build();
        }).toList();
    }

    // =========================
    // CORE COMPUTATION
    // =========================

    private Map<Long, List<CheckoutItemDTO>> groupItemsBySeller(List<CheckoutItemDTO> items) {
        Set<Long> productIds = items.stream().map(CheckoutItemDTO::getProductId).collect(Collectors.toSet());
        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, List<CheckoutItemDTO>> grouped = new HashMap<>();
        for (CheckoutItemDTO item : items) {
            Product p = productMap.get(item.getProductId());
            if (p == null) throw new ResourceNotFoundException("Product not found: " + item.getProductId());
            Long sellerUserId = p.getSellerProfile().getUser().getId();
            grouped.computeIfAbsent(sellerUserId, k -> new ArrayList<>()).add(item);
        }
        return grouped;
    }

    private CheckoutComputationResult computeForItems(List<CheckoutItemDTO> items, String shippingLocation,
            String couponCode) {
        return computeForItems(items, shippingLocation, couponCode, null);
    }

    private CheckoutComputationResult computeForItems(List<CheckoutItemDTO> items, String shippingLocation,
            String couponCode, Long userId) {
        Set<Long> productIds = items.stream().map(CheckoutItemDTO::getProductId).collect(Collectors.toSet());
        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));

        BigDecimal itemsTotal = BigDecimal.ZERO;
        List<OrderItemResponseDTO> responses = new ArrayList<>();

        for (CheckoutItemDTO itemDto : items) {
            Product p = productMap.get(itemDto.getProductId());
            if (p == null) continue;

            ProductVariant variant = null;
            if (itemDto.getVariantId() != null) {
                variant = productVariantRepository.findById(itemDto.getVariantId()).orElse(null);
            }

            validateProductAndVariantForCheckout(p, variant, itemDto.getQuantity());

            BigDecimal basePrice = resolveEffectiveUnitPrice(p);
            BigDecimal unitPrice = (variant != null) ? variant.getEffectivePrice(basePrice) : basePrice;
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemDto.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            itemsTotal = itemsTotal.add(lineTotal);

            java.util.Map<String, String> attrMap = new java.util.LinkedHashMap<>();
            String variantLabel = null;
            if (variant != null) {
                variantLabel = variant.getVariantLabel();
                variant.getAttributeValues().forEach(vav ->
                    attrMap.put(vav.getAttributeValue().getAttribute().getName(), vav.getAttributeValue().getValue()));
            }

            responses.add(OrderItemResponseDTO.builder()
                    .productId(p.getId())
                    .variantId(variant != null ? variant.getId() : null)
                    .sku(variant != null ? variant.getSku() : null)
                    .name(p.getName())
                    .brand(p.getBrand())
                    .imagePath(p.getImages().isEmpty() ? null : p.getImages().get(0).getImagePath())
                    .quantity(itemDto.getQuantity())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .variantAttributes(attrMap)
                    .variantLabel(variantLabel)
                    .manufactureDate(p.getManufactureDate())
                    .expiryDate(p.getExpiryDate())
                    .build());
        }

        BigDecimal shippingFee = orderAccountingService.calculateShippingFee(products, itemsTotal, shippingLocation);
        BigDecimal discountTotal = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isEmpty()) {
            discountTotal = promoCodeService.calculateDiscount(couponCode, items, userId).getAmount();
        }

        BigDecimal vatAmount = calculateIncludedVat(itemsTotal);
        BigDecimal grandTotal = itemsTotal.add(shippingFee).subtract(discountTotal);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) grandTotal = BigDecimal.ZERO;

        CheckoutComputationResult r = new CheckoutComputationResult();
        r.itemsTotal = itemsTotal;
        r.shippingFee = shippingFee;
        r.vatAmount = vatAmount;
        r.discountTotal = discountTotal;
        r.grandTotal = grandTotal;
        r.itemResponses = responses;
        r.productById = productMap;
        return r;
    }

    // =========================
    // HELPERS
    // =========================

    private BigDecimal calculateIncludedVat(BigDecimal taxInclusiveAmount) {
        if (taxInclusiveAmount == null || taxInclusiveAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return taxInclusiveAmount
                .subtract(taxInclusiveAmount.divide(new BigDecimal("1.13"), 2, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveEffectiveUnitPrice(Product p) {
        if (Boolean.TRUE.equals(p.getOnSale()) && p.getSalePrice() != null) return p.getSalePrice();
        return p.getPrice();
    }

    private Order getOrderOrFail(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private void assertOrderAccess(Order order, User actor) {
        if (actor.getRole() == Role.ADMIN) return;
        if (order.getUser() != null && order.getUser().getId().equals(actor.getId())) return;
        if (actor.getRole() == Role.SELLER && order.getItems().stream()
                .anyMatch(it -> it.getProduct() != null && it.getProduct().getSellerProfile() != null && it.getProduct().getSellerProfile().getUser().getId().equals(actor.getId()))) return;
        throw new AuthorizationException("Access denied");
    }

    private boolean sellerOwns(Order order, Long sellerId) {
        return order.getItems().stream().allMatch(it -> it.getProduct().getSellerProfile().getUser().getId().equals(sellerId));
    }

    @Transactional
    public OrderSummaryDTO updateSellerOrderStatus(Long orderId, OrderStatus status, User actor) {
        Order order = getOrderOrFail(orderId);
        if (actor.getRole() != Role.ADMIN) {
            if (actor.getRole() != Role.SELLER || !sellerOwns(order, actor.getId())) throw new AuthorizationException("Access denied");
        }
        return applyOrderStatusChange(order, status, "Status updated to " + status);
    }

    @Transactional
    public OrderSummaryDTO applySystemOrderStatusChange(Order order, OrderStatus status, String note) {
        return applyOrderStatusChange(order, status, note);
    }

    private OrderSummaryDTO updateSellerOwnedOrderStatus(Long orderId, Long sellerId, OrderStatus nextStatus, String note) {
        Order order = getOrderOrFail(orderId);
        if (!sellerOwns(order, sellerId)) throw new AuthorizationException("Access denied");
        return applyOrderStatusChange(order, nextStatus, note);
    }

    private OrderSummaryDTO applyOrderStatusChange(Order order, OrderStatus nextStatus, String note) {
        if (nextStatus == null) throw new BusinessValidationException("Status required");
        orderStatusService.validateTransition(order.getStatus(), nextStatus);
        order.setStatus(nextStatus);
        if (nextStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
            orderAccountingService.finalizeDeliveredOrder(order);
        } else if (nextStatus == OrderStatus.CANCELLED || nextStatus == OrderStatus.FAILED) {
            applyCancelledOrFailedSideEffects(order, nextStatus);
        } else if (nextStatus == OrderStatus.RETURNED) {
            applyReturnedSideEffects(order);
        }
        orderRepository.save(order);
        publishLoyaltyStatusEvent(order, nextStatus);
        synchronizeDeliveryShipment(order, nextStatus, note);
        sendOrderStatusNotifications(order, nextStatus, note);
        return toSummaryDTO(order, mapItems(order));
    }

    private void synchronizeDeliveryShipment(Order order, OrderStatus nextStatus, String note) {
        if (!EnumSet.of(
                OrderStatus.PACKED,
                OrderStatus.SHIPPED,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.CANCELLED,
                OrderStatus.FAILED,
                OrderStatus.RETURNED).contains(nextStatus)) {
            return;
        }

        try {
            shipmentServiceProvider.getObject().syncShipmentForOrderStatus(order, note);
        } catch (Exception ex) {
            log.error("Failed to synchronize shipment for order {} after status {}", order.getId(), nextStatus, ex);
            throw new BusinessValidationException("Order status changed, but courier shipment could not be synchronized. Please try again.");
        }
    }

    private void applyCancelledOrFailedSideEffects(Order order, OrderStatus nextStatus) {
        orderStockService.restoreStock(order);
        orderAccountingService.cancelAccounting(order);

        if (order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(nextStatus == OrderStatus.FAILED ? PaymentStatus.COD_FAILED : PaymentStatus.CANCELLED);
            return;
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setRefundPending(true);
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        } else {
            order.setPaymentStatus(nextStatus == OrderStatus.FAILED ? PaymentStatus.FAILED : PaymentStatus.CANCELLED);
        }
    }

    private void applyReturnedSideEffects(Order order) {
        orderStockService.restoreStock(order);
        orderAccountingService.cancelAccounting(order);

        if (order.getPaymentMethod() != PaymentMethod.COD && order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setRefundPending(true);
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        } else if (order.getPaymentMethod() == PaymentMethod.COD && order.getPaymentStatus() != PaymentStatus.COD_REMITTED) {
            order.setPaymentStatus(PaymentStatus.COD_FAILED);
        }
    }

    private void publishLoyaltyStatusEvent(Order order, OrderStatus status) {
        if (order.getUser() == null) {
            return;
        }
        if (status == OrderStatus.DELIVERED) {
            eventPublisher.publishEvent(new LoyaltyDomainEvent(LoyaltyEventType.ORDER_DELIVERED,
                    order.getId(), order.getUser().getId(), "order-delivered-" + order.getId()));
        } else if (status == OrderStatus.CANCELLED || status == OrderStatus.FAILED) {
            eventPublisher.publishEvent(new LoyaltyDomainEvent(LoyaltyEventType.ORDER_CANCELLED,
                    order.getId(), order.getUser().getId(), "order-cancelled-" + order.getId()));
        } else if (status == OrderStatus.RETURNED || status == OrderStatus.REFUNDED) {
            eventPublisher.publishEvent(new LoyaltyDomainEvent(LoyaltyEventType.ORDER_REFUNDED,
                    order.getId(), order.getUser().getId(), "order-refunded-" + order.getId()));
        }
    }

    private void sendOrderStatusNotifications(Order order, OrderStatus status, String note) {
        try {
            String ref = order.getCustomOrderId() != null ? order.getCustomOrderId() : String.valueOf(order.getId());
            emailService.sendOrderStatusUpdateEmail(
                    order.getCustomerEmail(), order.getCustomerName(), ref, status.name());
            notificationService.createNotification(
                    order.getUser(),
                    "Order Update",
                    "Order " + ref + " is " + status.name().replace("_", " "),
                    com.example.jhapcham.notification.domain.NotificationType.ORDER_UPDATE,
                    order.getId());
        } catch (Exception e) {
            log.error("Notification failed", e);
        }
    }

    private String estimateDelivery(String zone) {
        return "OUTSIDE".equalsIgnoreCase(zone) ? "3 to 5 days" : "1 to 2 days";
    }

    private PaymentMethod parsePaymentMethod(String raw) {
        try { return PaymentMethod.valueOf(raw.toUpperCase()); } catch (Exception e) { return PaymentMethod.COD; }
    }

    private PaymentStatus initialPaymentStatus(PaymentMethod pm) {
        return pm == PaymentMethod.COD ? PaymentStatus.PENDING_COD : PaymentStatus.REQUIRES_PAYMENT;
    }

    private void sendPlacementNotifications(Order order, User user) {
        String sellerEmail = null;
        String sellerName = null;
        if (!order.getItems().isEmpty()) {
            OrderItem first = order.getItems().get(0);
            if (first.getProduct() != null) {
                SellerProfile s = first.getProduct().getSellerProfile();
                if (s != null && s.getUser() != null) {
                    sellerEmail = s.getUser().getEmail();
                    sellerName = s.getUser().getFullName();
                }
            }
        }
        eventPublisher.publishEvent(new OrderPlacedEvent(
                order.getId(),
                order.getCustomOrderId(),
                user != null ? user.getId() : null,
                order.getCustomerEmail(),
                order.getCustomerName(),
                order.getGrandTotal(),
                sellerEmail,
                sellerName));
    }

    private void validateItems(CheckoutRequestDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) throw new BusinessValidationException("Items required");
    }

    private void validateProductAndVariantForCheckout(Product p, ProductVariant v, int q) {
        if (q <= 0) throw new BusinessValidationException("Quantity must be greater than zero");
        if (p.getStatus() != ProductStatus.ACTIVE) throw new BusinessValidationException("Product not active");
        boolean hasVariants = Boolean.TRUE.equals(p.getHasVariants()) ||
                (p.getVariants() != null && p.getVariants().stream().anyMatch(var -> Boolean.TRUE.equals(var.getActive())));
        if (hasVariants && v == null) {
            throw new BusinessValidationException("A variant must be selected for product: " + p.getName());
        }
        if (!hasVariants && v != null) {
            throw new BusinessValidationException("This product does not have variants: " + p.getName());
        }
        if (v != null) {
            if (v.getProduct() == null || !v.getProduct().getId().equals(p.getId())) {
                throw new BusinessValidationException("Selected variant does not belong to product: " + p.getName());
            }
            if (!Boolean.TRUE.equals(v.getActive())) {
                throw new BusinessValidationException("Selected variant is not active: " + v.getVariantLabel());
            }
            if (v.getStockQuantity() < q) throw new BusinessValidationException("Out of stock: " + v.getVariantLabel());
        } else {
            if (p.getStockQuantity() < q) throw new BusinessValidationException("Out of stock: " + p.getName());
        }
    }

    private void validateCustomerFields(CheckoutRequestDTO dto) {
        if (dto.getFullName() == null || dto.getEmail() == null || dto.getPhone() == null || dto.getAddress() == null)
            throw new BusinessValidationException("Customer details required");
    }

    private void validateCouponUser(String couponCode, Long userId) {
        if (couponCode != null && !couponCode.isBlank() && userId == null) {
            throw new BusinessValidationException("Login is required to use coupons");
        }
    }

    private String normalizeIdempotencyKey(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.trim();
        if (trimmed.length() > 120) {
            throw new BusinessValidationException("Idempotency key is too long");
        }
        return trimmed;
    }

    private String buildOrderIdempotencyKey(String baseKey, Long sellerUserId) {
        if (baseKey == null) {
            return null;
        }
        return baseKey + "#seller:" + sellerUserId;
    }

    private List<Order> findExistingIdempotentOrders(User user, String email, String idempotencyKey) {
        if (idempotencyKey == null) return List.of();
        String prefix = idempotencyKey + "#seller:";
        if (user != null) {
            List<Order> prefixed = orderRepository.findByUserIdAndIdempotencyKeyStartingWithOrderByCreatedAtAsc(
                    user.getId(), prefix);
            if (!prefixed.isEmpty()) {
                return prefixed;
            }
            return orderRepository.findByUserIdAndIdempotencyKeyOrderByCreatedAtAsc(user.getId(), idempotencyKey);
        }
        if (email != null && !email.isBlank()) {
            List<Order> prefixed = orderRepository.findByCustomerEmailIgnoreCaseAndIdempotencyKeyStartingWithOrderByCreatedAtAsc(
                    email.trim(), prefix);
            if (!prefixed.isEmpty()) {
                return prefixed;
            }
            return orderRepository.findByCustomerEmailIgnoreCaseAndIdempotencyKeyOrderByCreatedAtAsc(email.trim(),
                    idempotencyKey);
        }
        return List.of();
    }

    private void issueDeliveryOtp(Order order) {
        order.setDeliveryOtp(String.format("%06d", DELIVERY_OTP_RANDOM.nextInt(1_000_000)));
        order.setDeliveryOtpExpiry(LocalDateTime.now().plusMinutes(DELIVERY_OTP_EXPIRY_MINUTES));
    }

    private static class CheckoutComputationResult {
        BigDecimal itemsTotal;
        BigDecimal shippingFee;
        BigDecimal vatAmount;
        BigDecimal discountTotal;
        BigDecimal grandTotal;
        List<OrderItemResponseDTO> itemResponses;
        Map<Long, Product> productById;
    }

    private String buildAttrSnapshot(java.util.Map<String, String> attrs) {
        if (attrs == null || attrs.isEmpty()) return null;
        try { return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(attrs); } catch (Exception e) { return null; }
    }

    private OrderSummaryDTO toSummaryDTO(Order o, List<OrderItemResponseDTO> itemDTOs) {
        // Find seller info from the first item
        String sellerStore = "Platform Order";
        String sellerFull = "Jhapcham Admin";
        String sellerEmail = "admin@jhapcham.com";
        String sellerLogo = "";

        if (!o.getItems().isEmpty()) {
            OrderItem first = o.getItems().get(0);
            if (first.getProduct() != null && first.getProduct().getSellerProfile() != null) {
                SellerProfile s = first.getProduct().getSellerProfile();
                sellerStore = s.getStoreName();
                if (s.getUser() != null) {
                    sellerFull = s.getUser().getFullName();
                    sellerEmail = s.getUser().getEmail();
                }
                sellerLogo = s.getLogoImagePath();
            }
        }

        return OrderSummaryDTO.builder()
                .orderId(o.getId())
                .customOrderId(o.getCustomOrderId())
                .customerId(o.getUser() != null ? o.getUser().getId() : null)
                .status(o.getStatus())
                .customerName(o.getCustomerName())
                .customerEmail(o.getCustomerEmail())
                .customerPhone(o.getCustomerPhone())
                .shippingAddress(o.getShippingAddress())
                .shippingLocation(o.getShippingLocation())
                .customerAlternativePhone(o.getCustomerAlternativePhone())
                .deliveryTimePreference(o.getDeliveryTimePreference())
                .orderNote(o.getOrderNote())
                .itemsTotal(o.getItemsTotal())
                .shippingFee(o.getShippingFee())
                .vatAmount(o.getVatAmount())
                .discountTotal(o.getDiscountTotal())
                .grandTotal(o.getGrandTotal())
                .paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus())
                .paymentReference(o.getPaymentReference())
                .trackingId(o.getShipment() != null ? o.getShipment().getTrackingId() : null)
                .deliveryStatus(o.getShipment() != null ? o.getShipment().getStatus() : null)
                .items(itemDTOs)
                .appliedCoupon(o.getAppliedCoupon())
                .sellerStoreName(sellerStore)
                .sellerFullName(sellerFull)
                .sellerEmail(sellerEmail)
                .sellerLogoPath(sellerLogo)
                .createdAt(o.getCreatedAt())
                .deliveredAt(o.getDeliveredAt())
                .sellerGrossAmount(o.getSellerGrossAmount())
                .sellerShippingCharge(o.getSellerShippingCharge())
                .sellerNetAmount(o.getSellerNetAmount())
                .marketplaceCommission(o.getMarketplaceCommission())
                .sellerPromoDiscountAmount(o.getSellerPromoDiscountAmount())
                .platformSponsoredDiscountAmount(o.getPlatformSponsoredDiscountAmount())
                .inputVatAmount(o.getInputVatAmount())
                .outputVatAmount(o.getOutputVatAmount())
                .vatPayableAmount(o.getVatPayableAmount())
                .grossProfitAmount(o.getGrossProfitAmount())
                .netProfitAmount(o.getNetProfitAmount())
                .finalSellerEarnings(o.getFinalSellerEarnings())
                .deliveredBranch(o.getDeliveredBranch())
                .build();
    }

    private List<OrderItemResponseDTO> mapItems(Order order) {
        return order.getItems().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private OrderItemResponseDTO toResponseDTO(OrderItem i) {
        java.util.Map<String, String> attrMap = new java.util.LinkedHashMap<>();
        if (i.getVariantAttributesSnapshot() != null && !i.getVariantAttributesSnapshot().isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                attrMap = mapper.readValue(i.getVariantAttributesSnapshot(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, String>>() {});
            } catch (Exception e) {
                log.error("Failed to parse variant attributes snapshot", e);
            }
        }

        return OrderItemResponseDTO.builder()
                .id(i.getId())
                .productId(i.getProductIdSnapshot())
                .variantId(i.getVariant() != null ? i.getVariant().getId() : null)
                .sku(i.getVariantSkuSnapshot())
                .name(i.getProductNameSnapshot())
                .brand(i.getBrandSnapshot())
                .imagePath(resolveProductImage(i))
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .lineTotal(i.getLineTotal())
                .variantAttributes(attrMap)
                .variantLabel(i.getVariant() != null ? i.getVariant().getVariantLabel() : null)
                .manufactureDate(i.getManufactureDateSnapshot())
                .expiryDate(i.getExpiryDateSnapshot())
                .description(i.getProductDescriptionSnapshot())
                .specification(i.getSpecificationSnapshot())
                .features(i.getFeaturesSnapshot())
                .sellerStoreName(i.getProduct() != null && i.getProduct().getSellerProfile() != null ? i.getProduct().getSellerProfile().getStoreName() : null)
                .commissionRate(i.getCommissionPercentageSnapshot() != null ? i.getCommissionPercentageSnapshot() : orderAccountingService.getCommissionRate(i.getProduct() != null ? i.getProduct().getCategory() : "Others"))
                .commissionAmount(i.getCommissionAmountSnapshot())
                .buyingUnitPrice(i.getBuyingUnitPriceSnapshot())
                .buyingLineTotal(i.getBuyingLineTotalSnapshot())
                .sellingLineTotal(i.getSellingLineTotalSnapshot())
                .inputVatAmount(i.getInputVatAmountSnapshot())
                .outputVatAmount(i.getOutputVatAmountSnapshot())
                .vatPayableAmount(i.getVatPayableSnapshot())
                .sellerPromoDiscount(i.getSellerPromoDiscountSnapshot())
                .platformDiscount(i.getPlatformDiscountSnapshot())
                .commissionBase(i.getCommissionBaseSnapshot())
                .grossProfit(i.getGrossProfitSnapshot())
                .netProfit(i.getNetProfitSnapshot())
                .finalSellerEarning(i.getFinalSellerEarningSnapshot())
                .build();
    }

    private OrderListItemDTO toListItemDTO(Order o) {
        return OrderListItemDTO.builder()
                .orderId(o.getId())
                .customOrderId(o.getCustomOrderId())
                .customerName(o.getCustomerName())
                .grandTotal(o.getGrandTotal())
                .status(o.getStatus())
                .paymentStatus(o.getPaymentStatus())
                .createdAt(o.getCreatedAt())
                .appliedCoupon(o.getAppliedCoupon())
                .build();
    }

    private String resolveProductImage(OrderItem item) {
        if (item == null) return null;
        if (item.getImagePathSnapshot() != null) return item.getImagePathSnapshot();
        if (item.getProduct() != null && !item.getProduct().getImages().isEmpty()) {
            return item.getProduct().getImages().get(0).getImagePath();
        }
        return null;
    }
}
