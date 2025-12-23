package com.example.jhapcham.seller;

import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.common.FileStorageService;
import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderRepository;
import com.example.jhapcham.order.OrderStatus;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.product.ProductStatus;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerService {

        private final SellerProfileRepository sellerProfileRepository;
        private final ProductRepository productRepository;
        private final FileStorageService fileStorageService;
        private final UserRepository userRepository;
        private final OrderRepository orderRepository;
        private final FollowRepository followRepository;

        private static final String SELLER_LOGO_DIR = "seller_logos";

        // ----------------------------------------------------------
        // FETCH SELLER FULL PROFILE + PRODUCTS
        // ----------------------------------------------------------
        public SellerProfileResponseDTO getSellerProfile(Long sellerUserId) {

                SellerProfile profile = sellerProfileRepository.findByUserId(sellerUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

                List<Product> products = productRepository.findBySellerProfile(profile);
                long followerCount = followRepository.countBySeller(profile);

                return SellerProfileResponseDTO.from(profile, products, followerCount);
        }

        // ----------------------------------------------------------
        // UPDATE SELLER PROFILE + LOGO
        // ----------------------------------------------------------
        public SellerProfile updateSeller(Long sellerUserId, SellerUpdateRequestDTO dto) {

                SellerProfile profile = sellerProfileRepository.findByUserId(sellerUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

                // basic info
                if (dto.storeName() != null)
                        profile.setStoreName(dto.storeName());
                if (dto.address() != null)
                        profile.setAddress(dto.address());
                if (dto.description() != null)
                        profile.setDescription(dto.description());
                if (dto.about() != null)
                        profile.setAbout(dto.about());
                if (dto.insideValleyDeliveryFee() != null)
                        profile.setInsideValleyDeliveryFee(dto.insideValleyDeliveryFee());
                if (dto.outsideValleyDeliveryFee() != null)
                        profile.setOutsideValleyDeliveryFee(dto.outsideValleyDeliveryFee());
                if (dto.freeShippingEnabled() != null)
                        profile.setFreeShippingEnabled(dto.freeShippingEnabled());
                if (dto.freeShippingMinOrder() != null)
                        profile.setFreeShippingMinOrder(dto.freeShippingMinOrder());
                if (dto.contactNumber() != null)
                        profile.getUser().setContactNumber(dto.contactNumber());

                // save logo
                if (dto.logoImage() != null && !dto.logoImage().isEmpty()) {

                        String fileName = "seller_logo_" + profile.getId() + "_" + System.currentTimeMillis();

                        String path = fileStorageService.save(
                                        dto.logoImage(),
                                        SELLER_LOGO_DIR,
                                        fileName);

                        profile.setLogoImagePath(path);
                }

                return sellerProfileRepository.save(profile);
        }

        public SellerIncomeDTO getSellerIncome(Long sellerUserId) {

                User seller = userRepository.findById(sellerUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

                SellerProfile profile = sellerProfileRepository.findByUser(seller)
                                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

                return new SellerIncomeDTO(
                                profile.getId(),
                                profile.getTotalIncome(),
                                profile.getTotalShippingCost(),
                                profile.getNetIncome());
        }

        // ----------------------------------------------------------
        // COMPREHENSIVE DASHBOARD STATISTICS
        // ----------------------------------------------------------
        public SellerDashboardStatsDTO getDashboardStats(Long sellerUserId) {

                User seller = userRepository.findById(sellerUserId)
                                .orElseThrow(() -> new RuntimeException("Seller not found"));

                SellerProfile profile = sellerProfileRepository.findByUser(seller)
                                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

                // Get all orders for this seller
                List<Order> allOrders = orderRepository.findOrdersBySeller(sellerUserId);

                // Count orders by status
                Long deliveredCount = orderRepository.countOrdersBySellerAndStatus(sellerUserId, OrderStatus.DELIVERED);
                Long pendingCount = orderRepository.countOrdersBySellerAndStatus(sellerUserId, OrderStatus.NEW);
                Long processingCount = orderRepository.countOrdersBySellerAndStatus(sellerUserId,
                                OrderStatus.PROCESSING);
                Long shippedCount = orderRepository.countOrdersBySellerAndStatus(sellerUserId,
                                OrderStatus.SHIPPED_TO_BRANCH)
                                + orderRepository.countOrdersBySellerAndStatus(sellerUserId,
                                                OrderStatus.OUT_FOR_DELIVERY);
                Long canceledCount = orderRepository.countOrdersBySellerAndStatus(sellerUserId, OrderStatus.CANCELED);

                // Product counts
                Long totalProducts = productRepository.countBySellerProfile(profile);
                Long activeProducts = productRepository.countBySellerProfileAndStatus(profile, ProductStatus.ACTIVE);
                Long inactiveProducts = productRepository.countBySellerProfileAndStatus(profile,
                                ProductStatus.INACTIVE);

                // Last 30 days statistics
                LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
                List<Order> recentOrders = orderRepository.findOrdersBySellerSince(sellerUserId, thirtyDaysAgo);

                BigDecimal last30DaysIncome = BigDecimal.ZERO;
                long last30DaysOrderCount = 0;

                for (Order order : recentOrders) {
                        if (order.getStatus() == OrderStatus.DELIVERED && order.getSellerNetAmount() != null) {
                                last30DaysIncome = last30DaysIncome.add(order.getSellerNetAmount());
                                last30DaysOrderCount++;
                        }
                }

                return SellerDashboardStatsDTO.builder()
                                // Income metrics (from SellerProfile accumulated values)
                                .totalIncome(profile.getTotalIncome())
                                .totalShippingCost(profile.getTotalShippingCost())
                                .netIncome(profile.getNetIncome())

                                // Order counts
                                .totalOrders((long) allOrders.size())
                                .deliveredOrders(deliveredCount)
                                .pendingOrders(pendingCount)
                                .processingOrders(processingCount)
                                .shippedOrders(shippedCount)
                                .canceledOrders(canceledCount)

                                // Product metrics
                                .totalProducts(totalProducts)
                                .activeProducts(activeProducts)
                                .inactiveProducts(inactiveProducts)

                                // Recent activity
                                .last30DaysIncome(last30DaysIncome)
                                .last30DaysOrders(last30DaysOrderCount)
                                .build();
        }
}
