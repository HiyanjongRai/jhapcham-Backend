package com.example.jhapcham.admin;

import com.example.jhapcham.notification.NotificationService;
import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderRepository;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.product.ProductResponseDTO;
import com.example.jhapcham.product.ProductService;
import com.example.jhapcham.product.ProductStatus;
import com.example.jhapcham.report.dto.ReportResponseDTO;
import com.example.jhapcham.report.ReportService;
import com.example.jhapcham.report.ReportStatus;
import com.example.jhapcham.review.ReviewRepository;
import com.example.jhapcham.seller.SellerApplicationRepository;
import com.example.jhapcham.seller.ApplicationStatus;
import com.example.jhapcham.seller.SellerProfile;
import com.example.jhapcham.seller.SellerProfileRepository;
import com.example.jhapcham.user.model.AuthService;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.Status;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AuthService authService;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final ReportService reportService;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final OrderRepository orderRepository;
    private final com.example.jhapcham.seller.SellerApplicationService sellerApplicationService;
    private final SellerApplicationRepository sellerApplicationRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final com.example.jhapcham.wishlist.WishlistService wishlistService;
    private final com.example.jhapcham.order.OrderAccountingService orderAccountingService;

    public List<User> getAllUsers() {
        return authService.getAllUsers();
    }

    @Transactional(readOnly = true)
    public PlatformAnalyticsDTO getPlatformAnalytics() {
        try {
            // Use count queries — no full entity fetching to avoid lazy-load issues
            long totalUsers = userRepository.findByRole(Role.CUSTOMER).size();
            long totalSellers = sellerProfileRepository.count();
            long totalProducts = productRepository.count();
            long totalOrders = orderRepository.count();

            // For revenue, fetch only delivered orders (smaller dataset)
            double totalRevenue = orderRepository.findAll().stream()
                    .filter(o -> o.getStatus() == com.example.jhapcham.order.OrderStatus.DELIVERED)
                    .mapToDouble(o -> o.getGrandTotal() != null ? o.getGrandTotal().doubleValue() : 0.0)
                    .sum();

            double platformIncome = orderRepository.findAll().stream()
                    .filter(o -> o.getCommissionStatus() == com.example.jhapcham.order.CommissionStatus.PAID)
                    .mapToDouble(o -> o.getMarketplaceCommission() != null ? o.getMarketplaceCommission().doubleValue() : 0.0)
                    .sum();


            long pendingApplications = sellerApplicationRepository.findByStatus(ApplicationStatus.PENDING).size();

            long openReports = reportService.getAllReports().stream()
                    .filter(r -> r.getStatus() == ReportStatus.OPEN || r.getStatus() == ReportStatus.SELLER_REJECTED)
                    .count();

            long totalReviews = reviewRepository.count();

            // Trends for charts
            java.time.LocalDateTime thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30);
            java.util.Map<String, Long> dailyOrders = orderRepository.findByCreatedAtAfter(thirtyDaysAgo).stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            o -> o.getCreatedAt().toLocalDate().toString(),
                            java.util.stream.Collectors.counting()));

            java.time.LocalDateTime twelveMonthsAgo = java.time.LocalDateTime.now().minusMonths(12).withDayOfMonth(1).withHour(0).withMinute(0);
            java.util.Map<String, Double> monthlyRevenue = orderRepository.findByStatusAndCreatedAtAfter(com.example.jhapcham.order.OrderStatus.DELIVERED, twelveMonthsAgo).stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            o -> o.getCreatedAt().getYear() + "-" + String.format("%02d", o.getCreatedAt().getMonthValue()),
                            java.util.stream.Collectors.summingDouble(o -> o.getGrandTotal() != null ? o.getGrandTotal().doubleValue() : 0.0)));

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
    public List<com.example.jhapcham.order.OrderSummaryDTO> getAllOrders() {
        return orderRepository.findAllWithDetails().stream()
                .map(this::mapToOrderSummary)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(Long orderId, com.example.jhapcham.order.OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        if (status == com.example.jhapcham.order.OrderStatus.DELIVERED) {
            orderAccountingService.finalizeSellerAccounting(order);
            // Set due date for commission (1 week grace period)
            order.setCommissionDueDate(java.time.LocalDateTime.now().plusWeeks(1));
        } else if (status == com.example.jhapcham.order.OrderStatus.CANCELED) {
            orderAccountingService.cancelAccounting(order);
        }
        orderRepository.save(order);
    }

    @Transactional
    public void manuallyDeliverOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(com.example.jhapcham.order.OrderStatus.DELIVERED);
        orderAccountingService.finalizeSellerAccounting(order);
        
        // Settlement timing: Commission is due 7 days after delivery
        order.setCommissionDueDate(java.time.LocalDateTime.now().plusDays(7));
        
        orderRepository.save(order);
    }

    public List<com.example.jhapcham.review.ReviewResponseDTO> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(r -> com.example.jhapcham.review.ReviewResponseDTO.builder()
                        .id(r.getId())
                        .userName(r.getUser().getFullName())
                        .productName(r.getProduct().getName())
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    private com.example.jhapcham.order.OrderSummaryDTO mapToOrderSummary(com.example.jhapcham.order.Order o) {
        String storeName = "Global";
        if (o.getItems() != null && !o.getItems().isEmpty()) {
            com.example.jhapcham.order.OrderItem firstItem = o.getItems().get(0);
            if (firstItem.getProduct() != null && firstItem.getProduct().getSellerProfile() != null) {
                storeName = firstItem.getProduct().getSellerProfile().getStoreName();
            }
        }

        // Convert OrderItems to OrderItemResponseDTOs
        List<com.example.jhapcham.order.OrderItemResponseDTO> itemDTOs = o.getItems() != null 
            ? o.getItems().stream()
                .map(item -> com.example.jhapcham.order.OrderItemResponseDTO.builder()
                    .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                    .name(item.getProduct() != null ? item.getProduct().getName() : "Unknown Product")
                    .imagePath(item.getProduct() != null && item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty() ? item.getProduct().getImages().get(0).getImagePath() : null)
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build())
                .collect(java.util.stream.Collectors.toList())
            : new java.util.ArrayList<>();

        return com.example.jhapcham.order.OrderSummaryDTO.builder()
                .orderId(o.getId())
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

    public void setProductVisibility(Long productId, boolean visible) {
        productService.updateProductStatus(productId, visible ? ProductStatus.ACTIVE : ProductStatus.INACTIVE);
    }

    public List<ReportResponseDTO> getAllReports() {
        return reportService.getAllReports();
    }

    @Transactional
    public void createReportResolution(Long reportId, String status, String note) {
        try {
            if (status == null) status = "ADMIN_APPROVED";
            String cleanStatus = status.trim().toUpperCase()
                    .replace(" ", "_")
                    .replace("(", "")
                    .replace(")", "");

            boolean isApproved = false;
            if ("ADMIN_APPROVED".equals(cleanStatus) || "RESOLVED".equals(cleanStatus) || "APPROVED".equals(cleanStatus)) {
                isApproved = true;
            } else if ("REJECTED".equals(cleanStatus)) {
                isApproved = false;
            } else {
                isApproved = true; // default
            }

            com.example.jhapcham.report.dto.AdminActionDTO action = new com.example.jhapcham.report.dto.AdminActionDTO();
            action.setApproved(isApproved);
            action.setComment(note);
            action.setPayerType(com.example.jhapcham.refund.PayerType.SELLER);

            reportService.adminAction(reportId, action);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve report: " + e.getMessage(), e);
        }
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
                    .filter(o -> o.getStatus() == com.example.jhapcham.order.OrderStatus.DELIVERED)
                    .mapToDouble(o -> (o.getGrandTotal() != null ? o.getGrandTotal() : java.math.BigDecimal.ZERO)
                            .doubleValue())
                    .sum();

            int totalDelivered = (int) sellerOrders.stream()
                    .filter(o -> o.getStatus() == com.example.jhapcham.order.OrderStatus.DELIVERED)
                    .count();

            // Fetch documents from application if available
            com.example.jhapcham.seller.SellerApplication app = null;
            try {
                app = sellerApplicationRepository.findByUserId(sellerUserId).orElse(null);
            } catch (Exception e) {
                // Log but don't fail the whole list
                System.err.println("Error fetching application for seller " + sellerUserId + ": " + e.getMessage());
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
        }).toList();
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
                .filter(o -> o.getStatus() == com.example.jhapcham.order.OrderStatus.DELIVERED)
                .mapToDouble(
                        o -> (o.getGrandTotal() != null ? o.getGrandTotal() : java.math.BigDecimal.ZERO).doubleValue())
                .sum();

        int totalDelivered = (int) sellerOrders.stream()
                .filter(o -> o.getStatus() == com.example.jhapcham.order.OrderStatus.DELIVERED)
                .count();

        // Fetch documents from application if available
        com.example.jhapcham.seller.SellerApplication app = null;
        try {
            app = sellerApplicationRepository.findByUserId(sellerUserId).orElse(null);
        } catch (Exception e) {
            System.err.println("Error fetching application for seller " + sellerUserId + ": " + e.getMessage());
        }

        double totalCommission = sellerOrders.stream()
                .filter(o -> o.getStatus() == com.example.jhapcham.order.OrderStatus.DELIVERED)
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

    public List<com.example.jhapcham.seller.SellerApplication> getPendingApplications() {
        return sellerApplicationService.listPending();
    }

    @Transactional(readOnly = true)
    public List<com.example.jhapcham.order.OrderSummaryDTO> getOrdersBySeller(Long sellerId) {
        return orderRepository.findOrdersBySeller(sellerId).stream()
                .map(this::mapToOrderSummary)
                .collect(java.util.stream.Collectors.toList());
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

        List<com.example.jhapcham.order.OrderSummaryDTO> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToOrderSummary)
                .toList();

        List<ReportResponseDTO> reports = reportService.getAllReports().stream()
                .filter(r -> r.getCustomerId() != null && r.getCustomerId().equals(userId))
                .toList();

        List<com.example.jhapcham.product.ProductResponseDTO> wishlist = wishlistService.getWishlist(userId);

        double totalSpent = orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(o -> o.getStatus() == com.example.jhapcham.order.OrderStatus.DELIVERED)
                .mapToDouble(o -> (o.getGrandTotal() != null ? o.getGrandTotal() : java.math.BigDecimal.ZERO).doubleValue())
                .sum();

        String favPayment = orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(o -> o.getPaymentMethod() != null)
                .collect(java.util.stream.Collectors.groupingBy(o -> o.getPaymentMethod(), java.util.stream.Collectors.counting()))
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
                .reports(reports)                .wishlist(wishlist)
                .totalSpent(totalSpent)
                .totalOrders(orders.size())
                .favoritePaymentMethod(favPayment)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CommissionReportDTO> getCommissionReports() {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == com.example.jhapcham.order.OrderStatus.DELIVERED)
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
                    
                    if (o.getCommissionStatus() == com.example.jhapcham.order.CommissionStatus.UNPAID && o.getCommissionDueDate() != null) {
                        if (now.isAfter(o.getCommissionDueDate())) {
                            overdue = true;
                            long daysLate = java.time.Duration.between(o.getCommissionDueDate(), now).toDays();
                            long weeksLate = (daysLate / 7); 
                            
                            // Fine = 10% + 5% per week after the first week
                            double multiplier = 0.10 + (weeksLate * 0.05);
                            fine = baseComm.multiply(java.math.BigDecimal.valueOf(multiplier));
                        }
                    } else if (o.getCommissionStatus() == com.example.jhapcham.order.CommissionStatus.PAID) {
                        // If paid, use the static fine stored in the order (snapshot at payment time)
                        fine = o.getCommissionFineAmount() != null ? o.getCommissionFineAmount() : java.math.BigDecimal.ZERO;
                    }

                    return CommissionReportDTO.builder()
                        .orderId(o.getId())
                        .productName(o.getItems().isEmpty() ? "Order Sum" : (o.getItems().get(0).getProductNameSnapshot() != null ? o.getItems().get(0).getProductNameSnapshot() : "Product") + (o.getItems().size() > 1 ? "..." : ""))
                        .category(!o.getItems().isEmpty() && o.getItems().get(0).getProduct() != null ? o.getItems().get(0).getProduct().getCategory() : "Others")
                        .sellerStoreName(store)
                        .sellerEmail(email)
                        .sellerPhone(phone)
                        .saleAmount(o.getGrandTotal() != null ? o.getGrandTotal() : java.math.BigDecimal.ZERO)
                        .commissionRate(!o.getItems().isEmpty() && o.getItems().get(0).getCommissionPercentageSnapshot() != null ? o.getItems().get(0).getCommissionPercentageSnapshot() : Double.valueOf(10.0))
                        .commissionEarned(baseComm)
                        .fineAmount(fine)
                        .status(o.getCommissionStatus() != null ? o.getCommissionStatus() : com.example.jhapcham.order.CommissionStatus.PENDING)
                        .dueDate(o.getCommissionDueDate())
                        .createdAt(o.getCreatedAt() != null ? o.getCreatedAt() : java.time.LocalDateTime.now())
                        .isOverdue(overdue)
                        .reminderSent(o.isCommissionReminderSent())
                        .build();
                })
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .toList();
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
            com.example.jhapcham.notification.NotificationType.SYSTEM_ALERT,
            order.getId()
        );

        order.setCommissionReminderSent(true);
        orderRepository.save(order);
    }
}
