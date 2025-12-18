package com.example.jhapcham.admin;

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
import org.springframework.stereotype.Service;

import java.util.List;

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

    public void createReportResolution(Long reportId, String resolution) {
        reportService.updateReportStatus(reportId, ReportStatus.RESOLVED);
    }

    public SellerAdminDetailDTO getSellerDetails(Long sellerUserId) {
        User user = userRepository.findById(sellerUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SellerProfile profile = sellerProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Seller Profile not found"));

        int orderCount = orderRepository.findOrdersBySeller(sellerUserId).size();
        int productCount = productService.listProductsForSeller(sellerUserId).size();

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
                .build();
    }

    public List<com.example.jhapcham.seller.SellerApplication> getPendingApplications() {
        return sellerApplicationService.listPending();
    }

    public void approveSellerApplication(Long appId) {
        sellerApplicationService.approve(appId, "Approved by Admin");
    }

    public void rejectSellerApplication(Long appId) {
        sellerApplicationService.reject(appId, "Rejected by Admin");
    }
}
