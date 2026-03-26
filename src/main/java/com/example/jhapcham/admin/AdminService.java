package com.example.jhapcham.admin;

import com.example.jhapcham.notification.NotificationService;
import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderRepository;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.product.ProductResponseDTO;
import com.example.jhapcham.product.ProductService;
import com.example.jhapcham.product.ProductStatus;
import com.example.jhapcham.report.ReportDTO;
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

            long pendingApplications = sellerApplicationRepository.findByStatus(ApplicationStatus.PENDING).size();

            long openReports = reportService.getAllReports().stream()
                    .filter(r -> r.getStatus() == ReportStatus.NEW || r.getStatus() == ReportStatus.UNDER_INVESTIGATION)
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
                    .totalOrders(0).totalRevenue(0).pendingApplications(0)
                    .openReports(0).totalReviews(0)
                    .dailyOrders(new java.util.HashMap<>())
                    .monthlyRevenue(new java.util.HashMap<>())
                    .build();
        }
    }

    public List<com.example.jhapcham.order.OrderSummaryDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(o -> com.example.jhapcham.order.OrderSummaryDTO.builder()
                        .orderId(o.getId())
                        .status(o.getStatus())
                        .customerName(o.getCustomerName())
                        .grandTotal(o.getGrandTotal())
                        .createdAt(o.getCreatedAt())
                        .paymentMethod(o.getPaymentMethod())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(Long orderId, com.example.jhapcham.order.OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
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

    public List<ReportDTO> getAllReports() {
        return reportService.getAllReports();
    }

    @Transactional
    public void createReportResolution(Long reportId, String status, String note) {
        try {
            if (status == null)
                status = "RESOLVED";
            String cleanStatus = status.trim().toUpperCase()
                    .replace(" ", "_")
                    .replace("(", "")
                    .replace(")", "");

            ReportStatus reportStatus;
            try {
                reportStatus = ReportStatus.valueOf(cleanStatus);
            } catch (IllegalArgumentException e) {
                // Fallback for unexpected status strings
                reportStatus = ReportStatus.RESOLVED;
            }

            // Update status through service
            reportService.updateReportStatus(reportId, reportStatus);

            // Fetch the entity carefully for notification
            com.example.jhapcham.report.Report report = reportService.getReportById(reportId);
            if (report != null && report.getReporter() != null) {
                String entityName = report.getReportedEntityName() != null ? report.getReportedEntityName()
                        : "Reported Item";
                String message = "Your report on " + entityName + " has been updated to: " + reportStatus + ".";
                if (note != null && !note.trim().isEmpty()) {
                    message += " Note: " + note;
                }

                // Truncate message if it exceeds DB column limit (1000 in Report/Notification)
                if (message.length() > 1000)
                    message = message.substring(0, 997) + "...";

                notificationService.createNotification(
                        report.getReporter(),
                        "Report Update: " + reportStatus,
                        message,
                        com.example.jhapcham.notification.NotificationType.SYSTEM_ALERT,
                        reportId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve report: " + e.getMessage(), e);
        }
    }

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
                    .build();
        }).toList();
    }

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
                .build();
    }

    public List<com.example.jhapcham.seller.SellerApplication> getPendingApplications() {
        return sellerApplicationService.listPending();
    }

    public List<com.example.jhapcham.order.OrderSummaryDTO> getOrdersBySeller(Long sellerId) {
        return orderRepository.findOrdersBySeller(sellerId).stream()
                .map(o -> com.example.jhapcham.order.OrderSummaryDTO.builder()
                        .orderId(o.getId())
                        .status(o.getStatus())
                        .customerName(o.getCustomerName())
                        .grandTotal(o.getGrandTotal())
                        .createdAt(o.getCreatedAt())
                        .paymentMethod(o.getPaymentMethod())
                        .build())
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
                .map(o -> com.example.jhapcham.order.OrderSummaryDTO.builder()
                        .orderId(o.getId())
                        .status(o.getStatus())
                        .customerName(o.getCustomerName())
                        .grandTotal(o.getGrandTotal())
                        .createdAt(o.getCreatedAt())
                        .paymentMethod(o.getPaymentMethod())
                        .build())
                .toList();

        List<ReportDTO> reports = reportService.getAllReports().stream()
                .filter(r -> r.getReporterId() != null && r.getReporterId().equals(userId))
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
                .reports(reports)
                .wishlist(wishlist)
                .totalSpent(totalSpent)
                .totalOrders(orders.size())
                .favoritePaymentMethod(favPayment)
                .build();
    }
}
