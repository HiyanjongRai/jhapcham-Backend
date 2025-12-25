package com.example.jhapcham.admin;

import com.example.jhapcham.notification.NotificationService;
import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderRepository;
import com.example.jhapcham.product.ProductResponseDTO;
import com.example.jhapcham.product.ProductService;
import com.example.jhapcham.product.ProductStatus;
import com.example.jhapcham.report.ReportDTO;
import com.example.jhapcham.report.ReportService;
import com.example.jhapcham.report.ReportStatus;
import com.example.jhapcham.seller.SellerProfile;
import com.example.jhapcham.seller.SellerProfileRepository;
import com.example.jhapcham.user.model.AuthService;
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
    private final ReportService reportService;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final OrderRepository orderRepository;
    private final com.example.jhapcham.seller.SellerApplicationService sellerApplicationService;
    private final NotificationService notificationService;

    public List<User> getAllUsers() {
        return authService.getAllUsers();
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

    public void approveSellerApplication(Long appId, String note) {
        sellerApplicationService.approve(appId, note);
    }

    public void rejectSellerApplication(Long appId, String note) {
        sellerApplicationService.reject(appId, note);
    }
}
