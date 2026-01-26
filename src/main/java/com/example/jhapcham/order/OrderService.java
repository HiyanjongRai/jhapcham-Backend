package com.example.jhapcham.order;

import com.example.jhapcham.cart.CartItem;
import com.example.jhapcham.cart.CartItemRepository;
import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.activity.ActivityType;
import com.example.jhapcham.activity.UserActivityService;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.seller.SellerProfile;
import com.example.jhapcham.seller.SellerProfileRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final UserActivityService userActivityService;

    // Injected specialized services
    private final OrderStockService orderStockService;
    private final OrderAccountingService orderAccountingService;
    private final OrderStatusService orderStatusService;
    private final com.example.jhapcham.notification.NotificationService notificationService;

    // =========================
    // PREVIEW ORDER
    // =========================
    @Transactional
    public OrderPreviewDTO previewOrder(CheckoutRequestDTO dto) {
        validateItems(dto);

        // Group items by Seller to simulate the split
        Map<Long, List<CheckoutItemDTO>> itemsBySeller = groupItemsBySeller(dto.getItems());

        BigDecimal totalItemsCost = BigDecimal.ZERO;
        BigDecimal totalShipping = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalGrand = BigDecimal.ZERO;
        List<OrderItemResponseDTO> allItemResponses = new ArrayList<>();

        for (List<CheckoutItemDTO> sellerItems : itemsBySeller.values()) {
            CheckoutComputationResult data = computeForItems(sellerItems, dto.getShippingLocation());

            totalItemsCost = totalItemsCost.add(data.itemsTotal);
            totalShipping = totalShipping.add(data.shippingFee);
            totalDiscount = totalDiscount.add(data.discountTotal);
            totalGrand = totalGrand.add(data.grandTotal);
            allItemResponses.addAll(data.itemResponses);
        }

        return OrderPreviewDTO.builder()
                .items(allItemResponses)
                .itemsTotal(totalItemsCost)
                .shippingFee(totalShipping)
                .discountTotal(totalDiscount)
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

        PaymentMethod paymentMethod = parsePaymentMethod(dto.getPaymentMethod());

        // Group items by Seller to split orders
        Map<Long, List<CheckoutItemDTO>> itemsBySeller = groupItemsBySeller(dto.getItems());

        List<OrderSummaryDTO> summaries = new ArrayList<>();

        for (Map.Entry<Long, List<CheckoutItemDTO>> entry : itemsBySeller.entrySet()) {
            List<CheckoutItemDTO> sellerItems = entry.getValue();

            // Calculate for this specific seller's order
            CheckoutComputationResult data = computeForItems(sellerItems, dto.getShippingLocation());

            Order order = Order.builder()
                    .user(user)
                    .customerName(dto.getFullName())
                    .customerPhone(dto.getPhone())
                    .customerEmail(dto.getEmail())
                    .shippingAddress(dto.getAddress())
                    .shippingLocation(dto.getShippingLocation())
                    .customerAlternativePhone(dto.getAlternativePhone()) // NEW
                    .deliveryTimePreference(dto.getDeliveryTimePreference()) // NEW
                    .orderNote(dto.getOrderNote()) // NEW
                    .paymentMethod(paymentMethod)
                    .status(OrderStatus.NEW)
                    .itemsTotal(data.itemsTotal)
                    .shippingFee(data.shippingFee)
                    .discountTotal(data.discountTotal)
                    .grandTotal(data.grandTotal)
                    .createdAt(LocalDateTime.now())
                    // Initialize accounting fields to ZERO
                    .sellerGrossAmount(BigDecimal.ZERO)
                    .sellerShippingCharge(BigDecimal.ZERO)
                    .sellerNetAmount(BigDecimal.ZERO)
                    .sellerAccounted(false)
                    .build();

            orderRepository.save(order);

            for (OrderItemResponseDTO r : data.itemResponses) {
                Product product = data.productById.get(r.getProductId());

                // Use specialized stock service
                orderStockService.deductStock(product, r.getQuantity());

                OrderItem item = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .productIdSnapshot(product.getId())
                        .productNameSnapshot(product.getName())
                        .brandSnapshot(product.getBrand())
                        .imagePathSnapshot(r.getImagePath())
                        .quantity(r.getQuantity())
                        .unitPrice(r.getUnitPrice())
                        .lineTotal(r.getLineTotal())
                        .selectedColorSnapshot(r.getSelectedColor())
                        .selectedStorageSnapshot(r.getSelectedStorage())
                        .manufactureDateSnapshot(product.getManufactureDate())
                        .expiryDateSnapshot(product.getExpiryDate())
                        .productDescriptionSnapshot(product.getDescription())
                        .specificationSnapshot(product.getSpecification())
                        .featuresSnapshot(product.getFeatures())
                        .storageSpecSnapshot(product.getStorageSpec())
                        .colorOptionsSnapshot(product.getColorOptions())
                        .build();

                orderItemRepository.save(item);
                order.addItem(item);

                // Unified activity logging
                if (user != null) {
                    userActivityService.recordActivity(user.getId(), product.getId(), ActivityType.ORDER,
                            "Bought " + r.getQuantity() + " item(s)");
                }
            }

            orderRepository.save(order);
            summaries.add(toSummaryDTO(order, mapItems(order)));
            log.info("Order {} placed successfully for customer {}", order.getId(), dto.getFullName());
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
            it.setQuantity(c.getQuantity());
            it.setSelectedColor(c.getSelectedColor());
            it.setSelectedStorage(c.getSelectedStorage());
            checkoutItems.add(it);
        }

        CheckoutRequestDTO checkout = new CheckoutRequestDTO();
        checkout.setUserId(user.getId());
        checkout.setFullName(dto.getFullName());
        checkout.setPhone(dto.getPhone());
        checkout.setEmail(dto.getEmail());
        checkout.setAddress(dto.getAddress());
        checkout.setAlternativePhone(dto.getAlternativePhone()); // NEW
        checkout.setDeliveryTimePreference(dto.getDeliveryTimePreference()); // NEW
        checkout.setOrderNote(dto.getOrderNote()); // NEW
        checkout.setShippingLocation(dto.getShippingLocation());
        checkout.setPaymentMethod(dto.getPaymentMethod());
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
        Order order = getOrderOrFail(orderId);
        if (!sellerOwns(order, sellerId)) {
            throw new AuthorizationException("You do not have permission to process this order");
        }

        orderStatusService.validateTransition(order.getStatus(), OrderStatus.PROCESSING);
        order.setStatus(OrderStatus.PROCESSING);

        orderRepository.save(order);
        log.info("Seller {} moved order {} to PROCESSING", sellerId, orderId);

        // Notify Customer
        try {
            notificationService.createNotification(
                    order.getUser(),
                    "Order Update: Processing",
                    "Your order #" + order.getId() + " is now being processed by the merchant.",
                    com.example.jhapcham.notification.NotificationType.ORDER_UPDATE,
                    order.getId());
        } catch (Exception e) {
            log.error("Failed to notify customer of order processing", e);
        }

        return toSummaryDTO(order, mapItems(order));
    }

    @Transactional
    public OrderSummaryDTO sellerAssignBranch(Long orderId, Long sellerId, AssignBranchDTO dto) {
        Order order = getOrderOrFail(orderId);
        if (!sellerOwns(order, sellerId)) {
            throw new AuthorizationException("You do not have permission to assign branches to this order");
        }

        orderStatusService.validateTransition(order.getStatus(), OrderStatus.SHIPPED_TO_BRANCH);

        order.setAssignedBranch(DeliveryBranch.fromString(dto.getBranch()));
        order.setStatus(OrderStatus.SHIPPED_TO_BRANCH);

        orderRepository.save(order);
        log.info("Seller {} assigned branch {} to order {}", sellerId, dto.getBranch(), orderId);

        // Notify Customer
        try {
            notificationService.createNotification(
                    order.getUser(),
                    "Order Update: Shipped to Branch",
                    "Your order #" + order.getId() + " has been shipped to the " + dto.getBranch() + " branch.",
                    com.example.jhapcham.notification.NotificationType.ORDER_UPDATE,
                    order.getId());
        } catch (Exception e) {
            log.error("Failed to notify customer of branch assignment", e);
        }

        return toSummaryDTO(order, mapItems(order));
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

        orderStatusService.validateTransition(order.getStatus(), next);
        order.setStatus(next);

        if (next == OrderStatus.DELIVERED) {
            order.setDeliveredBranch(branch);
            // Use specialized accounting service
            orderAccountingService.applySellerAccounting(order);
        }

        orderRepository.save(order);
        log.info("Branch {} updated order {} status to {}", branchRaw, orderId, nextStatusRaw);

        // Notify Customer
        try {
            notificationService.createNotification(
                    order.getUser(),
                    "Order Update: " + nextStatusRaw,
                    "The status of your order #" + order.getId() + " has been updated to: " + nextStatusRaw + ".",
                    com.example.jhapcham.notification.NotificationType.ORDER_UPDATE,
                    order.getId());
        } catch (Exception e) {
            log.error("Failed to notify customer of branch status update", e);
        }

        return toSummaryDTO(order, mapItems(order));
    }

    @Transactional
    public OrderSummaryDTO sellerCancelOrder(Long orderId, Long sellerId) {
        Order order = getOrderOrFail(orderId);
        if (!sellerOwns(order, sellerId)) {
            throw new AuthorizationException("You do not have permission to cancel this order");
        }

        if (!orderStatusService.canCancel(order.getStatus())) {
            throw new BusinessValidationException("Cannot cancel order in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELED);
        // Use specialized stock service
        orderStockService.restoreStock(order);

        orderRepository.save(order);
        log.info("Seller {} cancelled order {}", sellerId, orderId);

        // Notify Customer
        try {
            notificationService.createNotification(
                    order.getUser(),
                    "Order Canceled by Merchant",
                    "We regret to inform you that your order #" + order.getId() + " was canceled by the merchant.",
                    com.example.jhapcham.notification.NotificationType.ORDER_UPDATE,
                    order.getId());
        } catch (Exception e) {
            log.error("Failed to notify customer of order cancellation", e);
        }

        return toSummaryDTO(order, mapItems(order));
    }

    @Transactional
    public OrderSummaryDTO customerCancelOrder(Long orderId, Long userId) {
        Order order = getOrderOrFail(orderId);
        if (order.getUser() == null || !order.getUser().getId().equals(userId)) {
            throw new AuthorizationException("You do not have permission to cancel this order");
        }

        if (!orderStatusService.canCancel(order.getStatus())) {
            throw new BusinessValidationException("Order cannot be canceled now");
        }

        order.setStatus(OrderStatus.CANCELED);
        // Use specialized stock service
        orderStockService.restoreStock(order);

        orderRepository.save(order);
        log.info("Customer {} cancelled order {}", userId, orderId);
        return toSummaryDTO(order, mapItems(order));
    }

    // =========================
    // FETCH (STAYS MOSTLY THE SAME)
    // =========================
    public OrderSummaryDTO getOrder(Long orderId) {
        Order order = getOrderOrFail(orderId);
        return toSummaryDTO(order, mapItems(order));
    }

    public List<OrderSummaryDTO> getOrdersForUser(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream().map(o -> toSummaryDTO(o, mapItems(o))).collect(Collectors.toList());
    }

    public List<OrderListItemDTO> getOrdersForUserSimple(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream().map(o -> OrderListItemDTO.builder()
                .orderId(o.getId())
                .status(o.getStatus())
                .grandTotal(o.getGrandTotal())
                .totalItems(o.getItems().size())
                .createdAt(o.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderListItemDTO> getOrdersForSeller(Long sellerUserId) {
        log.info("Fetching orders for sellerUserId: {}", sellerUserId);
        List<Order> orders = orderRepository.findOrdersBySeller(sellerUserId);
        log.info("Found {} orders for seller", orders.size());

        return orders.stream().map(o -> {
            List<OrderItem> sellerItems = o.getItems().stream()
                    .filter(i -> i.getProduct() != null &&
                            i.getProduct().getSellerProfile() != null &&
                            i.getProduct().getSellerProfile().getUser() != null &&
                            i.getProduct().getSellerProfile().getUser().getId().equals(sellerUserId))
                    .collect(Collectors.toList());

            log.info("Order {}: found {} items for this seller", o.getId(), sellerItems.size());

            OrderItem firstItem = sellerItems.isEmpty() ? (!o.getItems().isEmpty() ? o.getItems().get(0) : null)
                    : sellerItems.get(0);

            String image = null;
            if (firstItem != null) {
                image = firstItem.getImagePathSnapshot();
                log.info("  Item {}: Snapshot image: {}", firstItem.getId(), image);
                if (image == null && firstItem.getProduct() != null && !firstItem.getProduct().getImages().isEmpty()) {
                    image = firstItem.getProduct().getImages().get(0).getImagePath();
                    log.info("  Item {}: Fallback product image: {}", firstItem.getId(), image);
                }
            }

            return OrderListItemDTO.builder()
                    .orderId(o.getId())
                    .status(o.getStatus())
                    .grandTotal(o.getGrandTotal())
                    .totalItems(o.getItems().size())
                    .sellerGrossAmount(o.getSellerGrossAmount())
                    .sellerShippingCharge(o.getSellerShippingCharge())
                    .sellerNetAmount(o.getSellerNetAmount())
                    .deliveredBranch(o.getDeliveredBranch())
                    .assignedBranch(o.getAssignedBranch())
                    .createdAt(o.getCreatedAt())
                    .customerName(o.getCustomerName())
                    .customerPhone(o.getCustomerPhone())
                    .orderNote(o.getOrderNote())
                    .deliveryTimePreference(o.getDeliveryTimePreference())
                    .productNames(sellerItems.stream()
                            .map(OrderItem::getProductNameSnapshot)
                            .collect(Collectors.joining(", ")))
                    .productImage(image)
                    .customerProfileImagePath(o.getUser() != null ? o.getUser().getProfileImagePath() : null)
                    .build();
        }).collect(Collectors.toList());
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
            if (p == null)
                throw new ResourceNotFoundException("Product not found: " + item.getProductId());
            Long sellerUserId = p.getSellerProfile().getUser().getId();
            grouped.computeIfAbsent(sellerUserId, k -> new ArrayList<>()).add(item);
        }
        return grouped;
    }

    private CheckoutComputationResult computeForItems(List<CheckoutItemDTO> items, String shippingLocation) {
        Map<Long, Integer> quantityMap = items.stream()
                .collect(Collectors.toMap(CheckoutItemDTO::getProductId, CheckoutItemDTO::getQuantity));
        List<Product> products = productRepository.findAllById(quantityMap.keySet());

        if (products.size() != quantityMap.size())
            throw new ResourceNotFoundException("One or more products not found");

        BigDecimal itemsTotal = BigDecimal.ZERO;
        List<OrderItemResponseDTO> responses = new ArrayList<>();
        Map<Long, Product> productMap = new HashMap<>();

        for (Product p : products) {
            int qty = quantityMap.get(p.getId());
            BigDecimal unitPrice = resolveEffectiveUnitPrice(p);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            itemsTotal = itemsTotal.add(lineTotal);

            CheckoutItemDTO itemDto = items.stream().filter(i -> i.getProductId().equals(p.getId())).findFirst()
                    .orElse(null);

            responses.add(OrderItemResponseDTO.builder()
                    .productId(p.getId())
                    .name(p.getName())
                    .brand(p.getBrand())
                    .imagePath(p.getImages().isEmpty() ? null : p.getImages().get(0).getImagePath())
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .selectedColor(itemDto != null ? itemDto.getSelectedColor() : null)
                    .selectedStorage(itemDto != null ? itemDto.getSelectedStorage() : null)
                    .manufactureDate(p.getManufactureDate())
                    .expiryDate(p.getExpiryDate())
                    .build());
            productMap.put(p.getId(), p);
        }

        // Use specialized accounting service for shipping
        BigDecimal shippingFee = orderAccountingService.calculateShippingFee(products, itemsTotal, shippingLocation);
        BigDecimal grandTotal = itemsTotal.add(shippingFee);

        CheckoutComputationResult r = new CheckoutComputationResult();
        r.itemsTotal = itemsTotal;
        r.shippingFee = shippingFee;
        r.discountTotal = BigDecimal.ZERO;
        r.grandTotal = grandTotal;
        r.itemResponses = responses;
        r.productById = productMap;
        return r;
    }

    // =========================
    // HELPERS
    // =========================

    private List<OrderItemResponseDTO> mapItems(Order order) {
        return order.getItems().stream().map(this::toItemResponse).collect(Collectors.toList());
    }

    private OrderItemResponseDTO toItemResponse(OrderItem i) {
        return OrderItemResponseDTO.builder()
                .productId(i.getProductIdSnapshot())
                .name(i.getProductNameSnapshot())
                .brand(i.getBrandSnapshot())
                .imagePath(i.getImagePathSnapshot() != null ? i.getImagePathSnapshot()
                        : (i.getProduct() != null && !i.getProduct().getImages().isEmpty()
                                ? i.getProduct().getImages().get(0).getImagePath()
                                : null))
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .lineTotal(i.getLineTotal())
                .selectedColor(i.getSelectedColorSnapshot())
                .selectedStorage(i.getSelectedStorageSnapshot())
                .manufactureDate(i.getManufactureDateSnapshot())
                .expiryDate(i.getExpiryDateSnapshot())
                .description(i.getProductDescriptionSnapshot())
                .specification(i.getSpecificationSnapshot())
                .features(i.getFeaturesSnapshot())
                .storageSpec(i.getStorageSpecSnapshot())
                .colorOptions(i.getColorOptionsSnapshot())
                .build();
    }

    private OrderSummaryDTO toSummaryDTO(Order o, List<OrderItemResponseDTO> items) {
        return OrderSummaryDTO.builder()
                .orderId(o.getId())
                .status(o.getStatus())
                .customerName(o.getCustomerName())
                .customerPhone(o.getCustomerPhone())
                .customerEmail(o.getCustomerEmail())
                .shippingAddress(o.getShippingAddress())
                .shippingLocation(o.getShippingLocation())
                .customerAlternativePhone(o.getCustomerAlternativePhone()) // NEW
                .deliveryTimePreference(o.getDeliveryTimePreference()) // NEW
                .orderNote(o.getOrderNote()) // NEW
                .paymentMethod(o.getPaymentMethod())
                .paymentReference(o.getPaymentReference())
                .itemsTotal(o.getItemsTotal())
                .shippingFee(o.getShippingFee())
                .discountTotal(o.getDiscountTotal())
                .grandTotal(o.getGrandTotal())
                .createdAt(o.getCreatedAt())
                .items(items)
                .customerProfileImagePath(o.getUser() != null ? o.getUser().getProfileImagePath() : null)
                .build();
    }

    private BigDecimal resolveEffectiveUnitPrice(Product p) {
        if (Boolean.TRUE.equals(p.getOnSale()) && p.getSalePrice() != null)
            return p.getSalePrice();
        return p.getPrice();
    }

    private Order getOrderOrFail(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private boolean sellerOwns(Order order, Long sellerId) {
        return order.getItems().stream()
                .allMatch(it -> it.getProduct().getSellerProfile().getUser().getId().equals(sellerId));
    }

    private String estimateDelivery(String zone) {
        return "OUTSIDE".equalsIgnoreCase(zone) ? "3 to 5 days" : "1 to 2 days";
    }

    private PaymentMethod parsePaymentMethod(String raw) {
        try {
            return PaymentMethod.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            return PaymentMethod.COD; // Fallback
        }
    }

    private void validateItems(CheckoutRequestDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty())
            throw new BusinessValidationException("No items in order");
    }

    private void validateCustomerFields(CheckoutRequestDTO dto) {
        if (dto.getFullName() == null || dto.getFullName().isBlank())
            throw new BusinessValidationException("Customer name is required");
        if (dto.getPhone() == null || dto.getPhone().isBlank())
            throw new BusinessValidationException("Customer phone is required");
        if (dto.getEmail() == null || dto.getEmail().isBlank())
            throw new BusinessValidationException("Customer email is required");
        if (dto.getAddress() == null || dto.getAddress().isBlank())
            throw new BusinessValidationException("Shipping address is required");
    }

    private static class CheckoutComputationResult {
        BigDecimal itemsTotal;
        BigDecimal shippingFee;
        BigDecimal discountTotal;
        BigDecimal grandTotal;
        List<OrderItemResponseDTO> itemResponses;
        Map<Long, Product> productById;
    }
}
