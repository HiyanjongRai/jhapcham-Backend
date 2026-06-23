package com.example.jhapcham.admin.application;

import com.example.jhapcham.notification.application.NotificationService;
import com.example.jhapcham.order.domain.Order;
import com.example.jhapcham.order.persistence.OrderRepository;
import com.example.jhapcham.order.domain.PaymentMethod;
import com.example.jhapcham.order.domain.PaymentStatus;
import com.example.jhapcham.product.persistence.ProductRepository;
import com.example.jhapcham.product.dto.ProductResponseDTO;
import com.example.jhapcham.product.application.ProductService;
import com.example.jhapcham.product.domain.ProductStatus;
import com.example.jhapcham.admin.dto.*;
import com.example.jhapcham.report.application.ReportService;
import com.example.jhapcham.report.domain.ReportStatus;
import com.example.jhapcham.review.persistence.ReviewRepository;
import com.example.jhapcham.seller.persistence.SellerApplicationRepository;
import com.example.jhapcham.seller.domain.ApplicationStatus;
import com.example.jhapcham.seller.domain.SellerProfile;
import com.example.jhapcham.seller.persistence.SellerProfileRepository;
import com.example.jhapcham.user.application.AuthService;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.user.domain.Status;
import com.example.jhapcham.user.domain.User;
import com.example.jhapcham.user.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final AuthService authService;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final ReportService reportService;
    private final com.example.jhapcham.report.persistence.ProductReportRepository productReportRepository;
    private final com.example.jhapcham.report.persistence.SellerReportRepository sellerReportRepository;
    private final com.example.jhapcham.report.persistence.CustomerReportRepository customerReportRepository;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final OrderRepository orderRepository;
    private final com.example.jhapcham.seller.application.SellerApplicationService sellerApplicationService;
    private final SellerApplicationRepository sellerApplicationRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final com.example.jhapcham.wishlist.application.WishlistService wishlistService;
    private final com.example.jhapcham.order.application.OrderAccountingService orderAccountingService;
    private final com.example.jhapcham.order.application.OrderStatusService orderStatusService;
    private final com.example.jhapcham.order.application.OrderStockService orderStockService;
    private final com.example.jhapcham.delivery.application.ShipmentService shipmentService;
    private final com.example.jhapcham.delivery.application.TrackingService trackingService;

    public List<User> getAllUsers() {
        return authService.getAllUsers();
    }

    @Transactional(readOnly = true)
    public PlatformAnalyticsDTO getPlatformAnalytics() {
        try {
            // Use count queries — no full entity fetching to avoid lazy-load issues
            long totalUsers = userRepository.countByRole(Role.CUSTOMER);
            long totalSellers = sellerProfileRepository.count();
            long totalProducts = productRepository.count();
            long totalOrders = orderRepository.count();

            double totalRevenue = orderRepository
                    .sumGrandTotalByStatus(com.example.jhapcham.order.domain.OrderStatus.DELIVERED)
                    .doubleValue();

            double platformIncome = orderRepository
                    .sumMarketplaceCommissionByCommissionStatus(com.example.jhapcham.order.domain.CommissionStatus.PAID)
                    .doubleValue();


            long pendingApplications = sellerApplicationRepository.findByStatus(ApplicationStatus.PENDING).size();

            long openReports = productReportRepository.countByStatusIn(java.util.List.of(ReportStatus.NEW, ReportStatus.UNDER_REVIEW, ReportStatus.NEEDS_MORE_INFO))
                    + sellerReportRepository.countByStatusIn(java.util.List.of(ReportStatus.NEW, ReportStatus.UNDER_REVIEW, ReportStatus.NEEDS_MORE_INFO))
                    + customerReportRepository.countByStatusIn(java.util.List.of(ReportStatus.NEW, ReportStatus.UNDER_REVIEW, ReportStatus.NEEDS_MORE_INFO));

            long totalReviews = reviewRepository.count();

            // Trends for charts
            java.time.LocalDateTime thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30);
            java.util.Map<String, Long> dailyOrders = orderRepository.countOrdersByDaySince(thirtyDaysAgo).stream()
                    .collect(Collectors.toMap(
                            row -> String.valueOf(row[0]),
                            row -> ((Number) row[1]).longValue()));

            java.time.LocalDateTime twelveMonthsAgo = java.time.LocalDateTime.now().minusMonths(12).withDayOfMonth(1).withHour(0).withMinute(0);
            java.util.Map<String, Double> monthlyRevenue = orderRepository
                    .sumRevenueByMonthSince(com.example.jhapcham.order.domain.OrderStatus.DELIVERED, twelveMonthsAgo).stream()
                    .collect(Collectors.toMap(
                            row -> String.valueOf(row[0]),
                            row -> ((Number) row[1]).doubleValue()));

            return PlatformAnalyticsDTO.builder()
                    .totalUsers(totalUsers)
                    .totalSellers(totalSellers)
                    .totalProducts(totalProducts)
                    .totalOrders(totalOrders)
                    .totalRevenue(totalRevenue)
                    .platformIncome(platformIncome)
                    .pendingApplications(pendingApplications)
                    .openReports(openReports)
                    .totalReviews(totalReviews)
                    .dailyOrders(dailyOrders)
                    .monthlyRevenue(monthlyRevenue)
                    .build();
        } catch (Exception e) {
            // Return safe defaults if any aggregation fails
            return PlatformAnalyticsDTO.builder()
                    .totalUsers(0).totalSellers(0).totalProducts(0)
                    .totalOrders(0).totalRevenue(0).platformIncome(0).pendingApplications(0)
                    .openReports(0).totalReviews(0)
                    .dailyOrders(new java.util.HashMap<>())
                    .monthlyRevenue(new java.util.HashMap<>())
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public List<com.example.jhapcham.order.dto.OrderSummaryDTO> getAllOrders() {
        return orderRepository.findAllWithDetails().stream()
                .map(this::mapToOrderSummary)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(Long orderId, com.example.jhapcham.order.domain.OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        orderStatusService.validateTransition(order.getStatus(), status);
        order.setStatus(status);
        if (status == com.example.jhapcham.order.domain.OrderStatus.DELIVERED) {
            if (order.getPaymentMethod() == PaymentMethod.COD) {
                order.setPaymentStatus(PaymentStatus.COD_COLLECTED);
            }
            orderAccountingService.finalizeDeliveredOrder(order);
            // Set due date for commission (1 week grace period)
            order.setCommissionDueDate(java.time.LocalDateTime.now().plusWeeks(1));
        } else if (status == com.example.jhapcham.order.domain.OrderStatus.CANCELLED
                || status == com.example.jhapcham.order.domain.OrderStatus.FAILED) {
            orderStockService.restoreStock(order);
            if (order.getPaymentMethod() != PaymentMethod.COD && order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setRefundPending(true);
                order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            } else if (order.getPaymentMethod() == PaymentMethod.COD
                    && status == com.example.jhapcham.order.domain.OrderStatus.FAILED) {
                order.setPaymentStatus(PaymentStatus.COD_FAILED);
            } else if (order.getPaymentStatus() != PaymentStatus.PAID) {
                order.setPaymentStatus(status == com.example.jhapcham.order.domain.OrderStatus.FAILED
                        ? PaymentStatus.FAILED
                        : PaymentStatus.CANCELLED);
            }
            orderAccountingService.cancelAccounting(order);
        } else if (status == com.example.jhapcham.order.domain.OrderStatus.RETURNED) {
            orderStockService.restoreStock(order);
            orderAccountingService.cancelAccounting(order);
            if (order.getPaymentMethod() != PaymentMethod.COD && order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setRefundPending(true);
                order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            } else if (order.getPaymentMethod() == PaymentMethod.COD
                    && order.getPaymentStatus() != PaymentStatus.COD_REMITTED) {
                order.setPaymentStatus(PaymentStatus.COD_FAILED);
            }
        }
        orderRepository.save(order);
        synchronizeShipment(order, status);
    }

    private void synchronizeShipment(Order order, com.example.jhapcham.order.domain.OrderStatus status) {
        com.example.jhapcham.delivery.domain.Shipment shipment = shipmentService.findByOrderId(order.getId());

        switch (status) {
            case PACKED -> shipmentService.createShipment(order.getId());
            case SHIPPED -> {
                if (shipment == null) {
                    shipmentService.createShipment(order.getId());
                    shipment = shipmentService.findByOrderId(order.getId());
                }
                if (shipment != null && (shipment.getStatus() == com.example.jhapcham.delivery.domain.DeliveryStatus.CREATED
                        || shipment.getStatus() == com.example.jhapcham.delivery.domain.DeliveryStatus.RIDER_ASSIGNED)) {
                    advanceShipmentToStatus(
                            shipment,
                            com.example.jhapcham.delivery.domain.DeliveryStatus.PICKED_UP,
                            "Admin advanced order to shipped");
                }
            }
            case OUT_FOR_DELIVERY -> {
                if (shipment == null) {
                    shipmentService.createShipment(order.getId());
                    shipment = shipmentService.findByOrderId(order.getId());
                }
                if (shipment != null && shipment.getStatus() != com.example.jhapcham.delivery.domain.DeliveryStatus.OUT_FOR_DELIVERY) {
                    advanceShipmentToStatus(
                            shipment,
                            com.example.jhapcham.delivery.domain.DeliveryStatus.OUT_FOR_DELIVERY,
                            "Admin advanced order to out for delivery");
                }
            }
            case DELIVERED -> {
                if (shipment == null) {
                    throw new RuntimeException("Cannot mark delivered without a shipment and courier OTP verification");
                }
                if (shipment != null && shipment.getStatus() != com.example.jhapcham.delivery.domain.DeliveryStatus.DELIVERED) {
                    advanceShipmentToStatus(
                            shipment,
                            com.example.jhapcham.delivery.domain.DeliveryStatus.DELIVERED,
                            "Admin marked order as delivered");
                }
            }
            case CANCELLED, FAILED -> {
                if (shipment != null) {
                    shipmentService.cancelShipmentForOrder(order.getId(), "Admin cancelled order");
                }
            }
            case RETURNED -> {
                if (shipment != null && shipment.getStatus() != com.example.jhapcham.delivery.domain.DeliveryStatus.RETURN_TO_SELLER) {
                    trackingService.updateStatus(
                            shipment.getTrackingId(),
                            com.example.jhapcham.delivery.domain.DeliveryStatus.RETURN_TO_SELLER,
                            shipment.getShippingLocation(),
                            "Admin marked order as returned",
                            null);
                }
            }
            default -> {
                // No delivery action needed.
            }
        }
    }

    private void advanceShipmentToStatus(com.example.jhapcham.delivery.domain.Shipment shipment,
                                         com.example.jhapcham.delivery.domain.DeliveryStatus targetStatus,
                                         String note) {
        com.example.jhapcham.delivery.domain.DeliveryStatus current = shipment.getStatus();
        while (current != targetStatus) {
            com.example.jhapcham.delivery.domain.DeliveryStatus next = switch (current) {
                case CREATED -> com.example.jhapcham.delivery.domain.DeliveryStatus.RIDER_ASSIGNED;
                case RIDER_ASSIGNED -> com.example.jhapcham.delivery.domain.DeliveryStatus.PICKED_UP;
                case PICKED_UP, DELAYED -> com.example.jhapcham.delivery.domain.DeliveryStatus.IN_TRANSIT;
                case IN_TRANSIT -> com.example.jhapcham.delivery.domain.DeliveryStatus.OUT_FOR_DELIVERY;
                case OUT_FOR_DELIVERY -> com.example.jhapcham.delivery.domain.DeliveryStatus.DELIVERED;
                default -> targetStatus;
            };

            trackingService.updateStatus(shipment.getTrackingId(), next, shipment.getShippingLocation(), note, null);
            shipment = shipmentService.findByOrderId(shipment.getOrder().getId());
            if (shipment == null) {
                return;
            }
            current = shipment.getStatus();
        }
    }

    public List<com.example.jhapcham.review.dto.ReviewResponseDTO> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(r -> com.example.jhapcham.review.dto.ReviewResponseDTO.builder()
                        .id(r.getId())
                        .userName(r.getUser().getFullName())
                        .productName(r.getProduct().getName())
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private com.example.jhapcham.order.dto.OrderSummaryDTO mapToOrderSummary(com.example.jhapcham.order.domain.Order o) {
        String storeName = "Global";
        if (o.getItems() != null && !o.getItems().isEmpty()) {
            com.example.jhapcham.order.domain.OrderItem firstItem = o.getItems().get(0);
            if (firstItem.getProduct() != null && firstItem.getProduct().getSellerProfile() != null) {
                storeName = firstItem.getProduct().getSellerProfile().getStoreName();
            }
        }

        // Convert OrderItems to OrderItemResponseDTOs
        List<com.example.jhapcham.order.dto.OrderItemResponseDTO> itemDTOs = o.getItems() != null 
            ? o.getItems().stream()
                .map(item -> com.example.jhapcham.order.dto.OrderItemResponseDTO.builder()
                    .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                    .name(item.getProduct() != null ? item.getProduct().getName() : "Unknown Product")
                    .imagePath(item.getProduct() != null && item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty() ? item.getProduct().getImages().get(0).getImagePath() : null)
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build())
                .collect(Collectors.toList())
            : new java.util.ArrayList<>();

        return com.example.jhapcham.order.dto.OrderSummaryDTO.builder()
                .orderId(o.getId())
                .customOrderId(o.getCustomOrderId())
                .customerId(o.getUser() != null ? o.getUser().getId() : null)
                .status(o.getStatus())
                .customerName(o.getCustomerName())
                .customerPhone(o.getCustomerPhone())
                .customerEmail(o.getUser() != null ? o.getUser().getEmail() : null)
                .customerAlternativePhone(o.getCustomerAlternativePhone())
                .customerProfileImagePath(o.getUser() != null ? o.getUser().getProfileImagePath() : null)
                .shippingAddress(o.getShippingAddress())
                .shippingLocation(o.getShippingLocation())
                .deliveryTimePreference(o.getDeliveryTimePreference())
                .orderNote(o.getOrderNote())
                .itemsTotal(o.getItemsTotal())
                .shippingFee(o.getShippingFee())
                .discountTotal(o.getDiscountTotal())
                .grandTotal(o.getGrandTotal())
                .createdAt(o.getCreatedAt())
                .paymentMethod(o.getPaymentMethod())
                .paymentReference(o.getPaymentReference())
                .items(itemDTOs)
                .sellerStoreName(storeName)
                .sellerGrossAmount(o.getSellerGrossAmount())
                .sellerShippingCharge(o.getSellerShippingCharge())
                .sellerNetAmount(o.getSellerNetAmount())
                .marketplaceCommission(o.getMarketplaceCommission())
                .deliveredBranch(o.getDeliveredBranch())
                .build();
    }


    @Transactional
    public void deleteReview(Long reviewId) {
        reviewRepository.deleteById(reviewId);
    }

    public void blockUser(Long userId) {
        authService.updateUserStatus(userId, Status.BLOCKED);
    }

    public void unblockUser(Long userId) {
        authService.updateUserStatus(userId, Status.ACTIVE);
    }

    public List<ProductResponseDTO> getAllProducts() {
        return productService.listAllProductsForAdmin();
    }

    public org.springframework.data.domain.Page<ProductResponseDTO> getAllProducts(org.springframework.data.domain.Pageable pageable) {
        return productService.listAllProductsForAdmin(pageable);
    }

    public void setProductVisibility(Long productId, boolean visible) {
        productService.updateProductStatus(null, productId, visible ? ProductStatus.ACTIVE : ProductStatus.INACTIVE);
    }




    @Transactional(readOnly = true)
    public List<SellerAdminDetailDTO> getAllSellersMetrics() {
        List<SellerProfile> profiles = sellerProfileRepository.findAll();
        return profiles.stream().map(profile -> {
            Long sellerUserId = profile.getUser().getId();
            User user = profile.getUser();

            List<Order> sellerOrders = orderRepository.findOrdersBySeller(sellerUserId);
            int totalOrders = sellerOrders.size();
            int totalProducts = productService.listProductsForSeller(sellerUserId).size();

            double totalIncome = sellerOrders.stream()
                    .filter(o -> o.getStatus() == com.example.jhapcham.order.domain.OrderStatus.DELIVERED)
                    .mapToDouble(o -> (o.getGrandTotal() != null ? o.getGrandTotal() : java.math.BigDecimal.ZERO)
                            .doubleValue())
                    .sum();

            int totalDelivered = (int) sellerOrders.stream()
                    .filter(o -> o.getStatus() == com.example.jhapcham.order.domain.OrderStatus.DELIVERED)
                    .count();

            // Fetch documents from application if available
            com.example.jhapcham.seller.domain.SellerApplication app = null;
            try {
                app = sellerApplicationRepository.findByUserId(sellerUserId).orElse(null);
            } catch (Exception e) {
                log.warn("Error fetching application for seller {}: {}", sellerUserId, e.getMessage());
            }

            return SellerAdminDetailDTO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .contactNumber(user.getContactNumber())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .storeName(profile.getStoreName())
                    .totalOrders(totalOrders)
                    .totalProducts(totalProducts)
                    .totalIncome(totalIncome)
                    .totalDelivered(totalDelivered)
                    .idDocumentPath(app != null ? app.getIdDocumentPath() : null)
                    .businessLicensePath(app != null ? app.getBusinessLicensePath() : null)
                    .taxCertificatePath(app != null ? app.getTaxCertificatePath() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SellerAdminDetailDTO getSellerDetails(@NonNull Long sellerUserId) {
        User user = userRepository.findById(Objects.requireNonNull(sellerUserId, "Seller user ID cannot be null"))
                .orElseThrow(() -> new RuntimeException("User not found"));

        SellerProfile profile = sellerProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Seller Profile not found"));

        List<Order> sellerOrders = orderRepository.findOrdersBySeller(sellerUserId);
        int orderCount = sellerOrders.size();
        int productCount = productService.listProductsForSeller(sellerUserId).size();

        double totalIncome = sellerOrders.stream()
                .filter(o -> o.getStatus() == com.example.jhapcham.order.domain.OrderStatus.DELIVERED)
                .mapToDouble(
                        o -> (o.getGrandTotal() != null ? o.getGrandTotal() : java.math.BigDecimal.ZERO).doubleValue())
                .sum();

        int totalDelivered = (int) sellerOrders.stream()
                .filter(o -> o.getStatus() == com.example.jhapcham.order.domain.OrderStatus.DELIVERED)
                .count();

        // Fetch documents from application if available
        com.example.jhapcham.seller.domain.SellerApplication app = null;
        try {
            app = sellerApplicationRepository.findByUserId(sellerUserId).orElse(null);
        } catch (Exception e) {
            log.warn("Error fetching application for seller {}: {}", sellerUserId, e.getMessage());
        }

        double totalCommission = sellerOrders.stream()
                .filter(o -> o.getStatus() == com.example.jhapcham.order.domain.OrderStatus.DELIVERED)
                .mapToDouble(o -> (o.getMarketplaceCommission() != null ? o.getMarketplaceCommission() : java.math.BigDecimal.ZERO).doubleValue())
                .sum();

        return SellerAdminDetailDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .contactNumber(user.getContactNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .storeName(profile.getStoreName())
                .totalOrders(orderCount)
                .totalProducts(productCount)
                .totalIncome(totalIncome)
                .totalDelivered(totalDelivered)
                .totalCommission(totalCommission)
                .idDocumentPath(app != null ? app.getIdDocumentPath() : null)
                .businessLicensePath(app != null ? app.getBusinessLicensePath() : null)
                .taxCertificatePath(app != null ? app.getTaxCertificatePath() : null)
                .build();
    }

    public List<com.example.jhapcham.seller.domain.SellerApplication> getPendingApplications() {
        return sellerApplicationService.listPending();
    }

    @Transactional(readOnly = true)
    public List<com.example.jhapcham.order.dto.OrderSummaryDTO> getOrdersBySeller(Long sellerId) {
        return orderRepository.findOrdersBySeller(sellerId).stream()
                .map(this::mapToOrderSummary)
                .collect(Collectors.toList());
    }

    public void approveSellerApplication(Long appId, String note) {
        sellerApplicationService.approve(appId, note);
    }

    public void rejectSellerApplication(Long appId, String note) {
        sellerApplicationService.reject(appId, note);
    }

    @Transactional(readOnly = true)
    public CustomerAdminDetailDTO getCustomerDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<com.example.jhapcham.order.dto.OrderSummaryDTO> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToOrderSummary)
                .collect(Collectors.toList());

        List<com.example.jhapcham.product.dto.ProductResponseDTO> wishlist = wishlistService.getWishlist(userId);

        double totalSpent = orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(o -> o.getStatus() == com.example.jhapcham.order.domain.OrderStatus.DELIVERED)
                .mapToDouble(o -> (o.getGrandTotal() != null ? o.getGrandTotal() : java.math.BigDecimal.ZERO).doubleValue())
                .sum();

        String favPayment = orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(o -> o.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(o -> o.getPaymentMethod(), Collectors.counting()))
                .entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(e -> e.getKey().name())
                .orElse("None");

        return CustomerAdminDetailDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .contactNumber(user.getContactNumber())
                .status(user.getStatus())
                .orders(orders)
                .wishlist(wishlist)
                .totalSpent(totalSpent)
                .totalOrders(orders.size())
                .favoritePaymentMethod(favPayment)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CommissionReportDTO> getCommissionReports() {
        return orderRepository.findByStatusOrderByCreatedAtDesc(com.example.jhapcham.order.domain.OrderStatus.DELIVERED).stream()
                .map(o -> {
                    String store = "Unknown";
                    String email = "N/A";
                    String phone = "N/A";
                    
                    if (!o.getItems().isEmpty() && o.getItems().get(0).getProduct() != null && o.getItems().get(0).getProduct().getSellerProfile() != null) {
                        SellerProfile sp = o.getItems().get(0).getProduct().getSellerProfile();
                        store = sp.getStoreName();
                        if (sp.getUser() != null) {
                            email = sp.getUser().getEmail();
                            phone = (sp.getUser().getContactNumber() != null && !sp.getUser().getContactNumber().isEmpty()) 
                                    ? sp.getUser().getContactNumber() : sp.getContactNumber();
                        }
                    }

                    // --- Penalty Logic ---
                    java.math.BigDecimal fine = java.math.BigDecimal.ZERO;
                    boolean overdue = false;
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    java.math.BigDecimal baseComm = o.getMarketplaceCommission() != null ? o.getMarketplaceCommission() : java.math.BigDecimal.ZERO;
                    
                    if (o.getCommissionStatus() == com.example.jhapcham.order.domain.CommissionStatus.UNPAID && o.getCommissionDueDate() != null) {
                        if (now.isAfter(o.getCommissionDueDate())) {
                            overdue = true;
                            long daysLate = java.time.Duration.between(o.getCommissionDueDate(), now).toDays();
                            long weeksLate = (daysLate / 7); 
                            
                            // Fine = 10% + 5% per week after the first week
                            double multiplier = 0.10 + (weeksLate * 0.05);
                            fine = baseComm.multiply(java.math.BigDecimal.valueOf(multiplier));
                        }
                    } else if (o.getCommissionStatus() == com.example.jhapcham.order.domain.CommissionStatus.PAID) {
                        // If paid, use the static fine stored in the order (snapshot at payment time)
                        fine = o.getCommissionFineAmount() != null ? o.getCommissionFineAmount() : java.math.BigDecimal.ZERO;
                    }
java.math.BigDecimal netSale = (o.getItemsTotal() != null ? o.getItemsTotal() : java.math.BigDecimal.ZERO)
                .subtract(o.getDiscountTotal() != null ? o.getDiscountTotal() : java.math.BigDecimal.ZERO)
                .subtract(o.getLoyaltyDiscountAmount() != null ? o.getLoyaltyDiscountAmount() : java.math.BigDecimal.ZERO);
                    return CommissionReportDTO.builder()
                        .orderId(o.getId())
                        .customOrderId(o.getCustomOrderId())
                        .transactionId(o.getPaymentReference())
                        .productName(o.getItems().isEmpty() ? "Order Sum" : (o.getItems().get(0).getProductNameSnapshot() != null ? o.getItems().get(0).getProductNameSnapshot() : "Product") + (o.getItems().size() > 1 ? "..." : ""))
                        .category(!o.getItems().isEmpty() && o.getItems().get(0).getProduct() != null ? o.getItems().get(0).getProduct().getCategory() : "Others")
                        .sellerStoreName(store)
                        .sellerEmail(email)
                        .sellerPhone(phone)
                        .saleAmount(netSale)
                        .commissionRate(!o.getItems().isEmpty() && o.getItems().get(0).getCommissionPercentageSnapshot() != null ? o.getItems().get(0).getCommissionPercentageSnapshot() : Double.valueOf(10.0))
                        .commissionEarned(baseComm)
                        .fineAmount(fine)
                        .status(o.getCommissionStatus() != null ? o.getCommissionStatus() : com.example.jhapcham.order.domain.CommissionStatus.PENDING)
                        .dueDate(o.getCommissionDueDate())
                        .createdAt(o.getCreatedAt() != null ? o.getCreatedAt() : java.time.LocalDateTime.now())
                        .isOverdue(overdue)
                        .reminderSent(o.isCommissionReminderSent())
                        .vatAmount(o.getVatAmount() != null ? o.getVatAmount() : java.math.BigDecimal.ZERO)
                        .discountTotal(o.getDiscountTotal() != null ? o.getDiscountTotal() : java.math.BigDecimal.ZERO)
                        .netAmount(o.getSellerNetAmount() != null ? o.getSellerNetAmount() : java.math.BigDecimal.ZERO)
                        .sellerPromoDiscountAmount(o.getSellerPromoDiscountAmount() != null ? o.getSellerPromoDiscountAmount() : java.math.BigDecimal.ZERO)
                        .platformSponsoredDiscountAmount(o.getPlatformSponsoredDiscountAmount() != null ? o.getPlatformSponsoredDiscountAmount() : java.math.BigDecimal.ZERO)
                        .inputVatAmount(o.getInputVatAmount() != null ? o.getInputVatAmount() : java.math.BigDecimal.ZERO)
                        .outputVatAmount(o.getOutputVatAmount() != null ? o.getOutputVatAmount() : java.math.BigDecimal.ZERO)
                        .vatPayableAmount(o.getVatPayableAmount() != null ? o.getVatPayableAmount() : java.math.BigDecimal.ZERO)
                        .grossProfitAmount(o.getGrossProfitAmount() != null ? o.getGrossProfitAmount() : java.math.BigDecimal.ZERO)
                        .netProfitAmount(o.getNetProfitAmount() != null ? o.getNetProfitAmount() : java.math.BigDecimal.ZERO)
                        .finalSellerEarnings(o.getFinalSellerEarnings() != null ? o.getFinalSellerEarnings() : java.math.BigDecimal.ZERO)
                        .build();
                })
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void sendCommissionReminder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getItems().isEmpty() || order.getItems().get(0).getProduct() == null) return;
        
        SellerProfile sp = order.getItems().get(0).getProduct().getSellerProfile();
        if (sp == null || sp.getUser() == null) return;

        java.math.BigDecimal totalDue = order.getMarketplaceCommission();
        // Recalculate fine for message
        if (order.getCommissionDueDate() != null && java.time.LocalDateTime.now().isAfter(order.getCommissionDueDate())) {
            long daysLate = java.time.Duration.between(order.getCommissionDueDate(), java.time.LocalDateTime.now()).toDays();
            double multiplier = 0.10 + ((daysLate / 7) * 0.05);
            totalDue = totalDue.add(order.getMarketplaceCommission().multiply(java.math.BigDecimal.valueOf(multiplier)));
        }

        notificationService.createNotification(
            sp.getUser(),
            "URGENT: Settlement Required",
            "Your commission for Order #" + order.getId() + " is overdue. Total amount including late fines: Rs. " + totalDue.setScale(2, java.math.RoundingMode.HALF_UP).toString(),
            com.example.jhapcham.notification.domain.NotificationType.SYSTEM_ALERT,
            order.getId()
        );

        order.setCommissionReminderSent(true);
        orderRepository.save(order);
    }
}
