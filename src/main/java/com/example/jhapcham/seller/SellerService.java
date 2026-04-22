package com.example.jhapcham.seller;

import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.common.FileStorageService;
import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderAccountingService;
import com.example.jhapcham.order.OrderRepository;
import com.example.jhapcham.order.OrderStatus;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.product.ProductStatus;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import org.springframework.transaction.annotation.Transactional;
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
        private final OrderAccountingService orderAccountingService;
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
        public SellerProfileResponseDTO updateSeller(Long sellerUserId, SellerUpdateRequestDTO dto) {

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

                SellerProfile saved = sellerProfileRepository.save(profile);
                List<Product> products = productRepository.findBySellerProfile(saved);

                // Re-sync shipping fields on all products that have no product-level override.
                // A product is considered to have a product-level override when its
                // insideValleyShipping or outsideValleyShipping differs from what the
                // profile currently specifies (i.e. it was explicitly set from the
                // product edit form).  Products that still match one of the old profile
                // defaults (or are null) are updated to the new profile values.
                boolean shippingChanged = dto.insideValleyDeliveryFee() != null
                        || dto.outsideValleyDeliveryFee() != null
                        || dto.freeShippingEnabled() != null
                        || dto.freeShippingMinOrder() != null;

                if (shippingChanged) {
                        for (Product p : products) {
                                // Skip products that have product-level free shipping override
                                if (Boolean.TRUE.equals(p.getFreeShipping())) {
                                        continue;
                                }

                                // Update inside valley shipping if no product-level override
                                if (dto.insideValleyDeliveryFee() != null) {
                                        p.setInsideValleyShipping(saved.getInsideValleyDeliveryFee());
                                }

                                // Update outside valley shipping if no product-level override
                                if (dto.outsideValleyDeliveryFee() != null) {
                                        p.setOutsideValleyShipping(saved.getOutsideValleyDeliveryFee());
                                }

                                // Always re-sync free shipping min order threshold
                                if (Boolean.TRUE.equals(saved.getFreeShippingEnabled())) {
                                        p.setSellerFreeShippingMinOrder(saved.getFreeShippingMinOrder());
                                } else {
                                        p.setSellerFreeShippingMinOrder(null);
                                }

                                productRepository.save(p);
                        }
                }

                long followerCount = followRepository.countBySeller(saved);

                return SellerProfileResponseDTO.from(saved, products, followerCount);
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
                                profile.getTotalCommission(),
                                profile.getNetIncome());
        }

        // ----------------------------------------------------------
        // COMPREHENSIVE DASHBOARD STATISTICS
        // ----------------------------------------------------------
        @Transactional
        public SellerDashboardStatsDTO getDashboardStats(Long sellerUserId) {
                User seller = userRepository.findById(sellerUserId)
                                .orElseThrow(() -> new RuntimeException("Seller not found"));

                java.util.Optional<SellerProfile> profileOpt = sellerProfileRepository.findByUser(seller);
                
                // If profile missing, return empty stats instead of 400
                if (profileOpt.isEmpty()) {
                    return SellerDashboardStatsDTO.builder()
                        .storeName("Merchant (Pending Setup)")
                        .totalIncome(BigDecimal.ZERO)
                        .totalShippingCost(BigDecimal.ZERO)
                        .totalCommission(BigDecimal.ZERO)
                        .netIncome(BigDecimal.ZERO)
                        .totalOrders(0L)
                        .deliveredOrders(0L)
                        .pendingOrders(0L)
                        .processingOrders(0L)
                        .shippedOrders(0L)
                        .canceledOrders(0L)
                        .totalProducts(0L)
                        .activeProducts(0L)
                        .inactiveProducts(0L)
                        .last30DaysIncome(BigDecimal.ZERO)
                        .last30DaysOrders(0L)
                        .weeklySales(java.util.Collections.nCopies(7, BigDecimal.ZERO))
                        .topSellingProducts(java.util.Collections.emptyList())
                        .build();
                }

                SellerProfile profile = profileOpt.get();
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

                // Weekly statistics (last 7 days)
                LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
                List<Order> weeklyOrders = orderRepository.findOrdersBySellerSince(sellerUserId, sevenDaysAgo);

                List<BigDecimal> weeklySales = new java.util.ArrayList<>(java.util.Arrays.asList(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
                ));
                LocalDateTime now = LocalDateTime.now();

                for (Order order : weeklyOrders) {
                        if (order.getStatus() == OrderStatus.DELIVERED && order.getSellerNetAmount() != null) {
                                long daysAgo = java.time.temporal.ChronoUnit.DAYS.between(order.getCreatedAt(), now);
                                if (daysAgo >= 0 && daysAgo < 7) {
                                        int index = 6 - (int) daysAgo;
                                        weeklySales.set(index, weeklySales.get(index).add(order.getSellerNetAmount()));
                                }
                        }
                }

                // Get some products to display as "Top Selling" (just first 4 for visual
                // consistency)
                List<ProductSummaryDTO> topSellingProducts = productRepository.findBySellerProfile(profile).stream()
                                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                                .limit(4)
                                .map(ProductSummaryDTO::from)
                                .collect(java.util.stream.Collectors.toList());

                return SellerDashboardStatsDTO.builder()
                                // Income metrics (from SellerProfile accumulated values)
                                .totalIncome(profile.getTotalIncome() != null ? profile.getTotalIncome() : BigDecimal.ZERO)
                                .totalShippingCost(profile.getTotalShippingCost() != null ? profile.getTotalShippingCost() : BigDecimal.ZERO)
                                .totalCommission(profile.getTotalCommission() != null ? profile.getTotalCommission() : BigDecimal.ZERO)
                                .netIncome(profile.getNetIncome() != null ? profile.getNetIncome() : BigDecimal.ZERO)

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
                                .storeName(profile.getStoreName())
                                .logoImagePath(profile.getLogoImagePath())
                                .profileImagePath(profile.getUser() != null ? profile.getUser().getProfileImagePath() : null)
                                .weeklySales(weeklySales)
                                .topSellingProducts(topSellingProducts)
                                .build();
        }

        public SellerCustomerDetailDTO getCustomerDetailsForSeller(Long sellerUserId, Long customerId) {
                User customer = userRepository.findById(customerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

                // Get only orders for THIS seller by THIS customer
                List<Order> sellerOrders = orderRepository.findOrdersBySeller(sellerUserId).stream()
                                .filter(o -> o.getUser() != null && o.getUser().getId().equals(customerId))
                                .collect(java.util.stream.Collectors.toList());

                BigDecimal totalSpent = sellerOrders.stream()
                                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                                .map(Order::getSellerNetAmount)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                LocalDateTime lastOrderDate = sellerOrders.isEmpty() ? null : sellerOrders.get(0).getCreatedAt();

                List<com.example.jhapcham.order.OrderSummaryDTO> summaries = sellerOrders.stream()
                                .map(o -> com.example.jhapcham.order.OrderSummaryDTO.builder()
                                                .orderId(o.getId())
                                                .status(o.getStatus())
                                                .grandTotal(o.getGrandTotal())
                                                .sellerNetAmount(o.getSellerNetAmount())
                                                .paymentMethod(o.getPaymentMethod())
                                                .createdAt(o.getCreatedAt())
                                                .build())
                                .collect(java.util.stream.Collectors.toList());

                return SellerCustomerDetailDTO.builder()
                                .userId(customer.getId())
                                .fullName(customer.getFullName())
                                .email(customer.getEmail())
                                .phone(customer.getContactNumber())
                                .profileImagePath(customer.getProfileImagePath())
                                .status(customer.getStatus())
                                .joinedAt(customer.getCreatedAt())
                                .totalSpentWithSeller(totalSpent)
                                .orderCountWithSeller((long) sellerOrders.size())
                                .lastOrderDate(lastOrderDate)
                                .orderHistory(summaries)
                                .build();
        }

        @Transactional
        public List<com.example.jhapcham.admin.CommissionReportDTO> getSellerCommissions(Long sellerUserId) {
                return orderRepository.findOrdersBySeller(sellerUserId).stream()
                                .filter(o -> o != null && o.getStatus() == com.example.jhapcham.order.OrderStatus.DELIVERED)
                                .map(o -> {
                                    // Force load lazy collection while in transaction
                                    if (o.getItems() != null) o.getItems().size();
                                    
                                    try {
                                        // Calculate commission specific to THIS seller (for multi-vendor support)
                                        java.math.BigDecimal sellerSpecificCommission = o.getItems().stream()
                                            .filter(i -> i.getProduct() != null && i.getProduct().getSellerProfile() != null && 
                                                         i.getProduct().getSellerProfile().getUser().getId().equals(sellerUserId))
                                            .map(i -> i.getCommissionAmountSnapshot() != null ? i.getCommissionAmountSnapshot() : java.math.BigDecimal.ZERO)
                                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                                        // Total fine should also be proportional or handled specifically 
                                        // For now, simpler to treat the order as the unit of penalty or proportional to this seller's share
                                        java.math.BigDecimal fine = java.math.BigDecimal.ZERO;
                                        boolean overdue = false;
                                        java.time.LocalDateTime now = java.time.LocalDateTime.now();
                                        
                                        if (o.getCommissionStatus() == com.example.jhapcham.order.CommissionStatus.UNPAID && o.getCommissionDueDate() != null) {
                                                if (now.isAfter(o.getCommissionDueDate())) {
                                                        overdue = true;
                                                        long daysLate = java.time.Duration.between(o.getCommissionDueDate(), now).toDays();
                                                        long weeksLate = (daysLate / 7); 
                                                        double multiplier = 0.10 + (weeksLate * 0.05);
                                                        fine = sellerSpecificCommission.multiply(java.math.BigDecimal.valueOf(multiplier));
                                                }
                                        } else if (o.getCommissionStatus() == com.example.jhapcham.order.CommissionStatus.PAID) {
                                                // If paid, use the proportion of fine amount
                                                BigDecimal totalOrigComm = o.getMarketplaceCommission() != null && o.getMarketplaceCommission().compareTo(BigDecimal.ZERO) > 0 ? o.getMarketplaceCommission() : sellerSpecificCommission;
                                                if (totalOrigComm.compareTo(BigDecimal.ZERO) > 0) {
                                                    BigDecimal share = sellerSpecificCommission.divide(totalOrigComm, 4, java.math.RoundingMode.HALF_UP);
                                                    fine = (o.getCommissionFineAmount() != null ? o.getCommissionFineAmount() : BigDecimal.ZERO).multiply(share);
                                                }
                                        }

                                        return com.example.jhapcham.admin.CommissionReportDTO.builder()
                                                        .orderId(o.getId())
                                                        .productName(o.getItems() == null || o.getItems().isEmpty() ? "Order Sum" : (o.getItems().get(0).getProductNameSnapshot() != null ? o.getItems().get(0).getProductNameSnapshot() : "Product") + (o.getItems().size() > 1 ? "..." : ""))
                                                        .category(o.getItems() != null && !o.getItems().isEmpty() && o.getItems().get(0).getProduct() != null ? o.getItems().get(0).getProduct().getCategory() : "Others")
                                                        .sellerStoreName("Your Store")
                                                        .saleAmount(o.getGrandTotal() != null ? o.getGrandTotal() : java.math.BigDecimal.ZERO)
                                                        .commissionRate(o.getItems() != null && !o.getItems().isEmpty() && o.getItems().get(0).getCommissionPercentageSnapshot() != null ? o.getItems().get(0).getCommissionPercentageSnapshot() : 10.0)
                                                        .commissionEarned(sellerSpecificCommission)
                                                        .fineAmount(fine)
                                                        .status(o.getCommissionStatus() != null ? o.getCommissionStatus() : com.example.jhapcham.order.CommissionStatus.PENDING)
                                                        .dueDate(o.getCommissionDueDate())
                                                        .createdAt(o.getCreatedAt() != null ? o.getCreatedAt() : java.time.LocalDateTime.now())
                                                        .isOverdue(overdue)
                                                        .reminderSent(o.isCommissionReminderSent())
                                                        .build();
                                    } catch (Exception e) {
                                        return com.example.jhapcham.admin.CommissionReportDTO.builder()
                                                .orderId(o.getId())
                                                .productName("Error in Record")
                                                .status(com.example.jhapcham.order.CommissionStatus.PENDING)
                                                .commissionEarned(BigDecimal.ZERO)
                                                .fineAmount(BigDecimal.ZERO)
                                                .createdAt(o.getCreatedAt() != null ? o.getCreatedAt() : java.time.LocalDateTime.now())
                                                .build();
                                    }
                                })
                                .sorted(java.util.Comparator.comparing(com.example.jhapcham.admin.CommissionReportDTO::getCreatedAt, 
                                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                                .toList();
        }

        @Transactional
        public void payCommission(Long sellerUserId, Long orderId) {
                Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
                // Verify order belongs to the seller
                boolean belongsToSeller = order.getItems().stream()
                        .anyMatch(i -> i.getProduct() != null && i.getProduct().getSellerProfile() != null && 
                                       i.getProduct().getSellerProfile().getUser().getId().equals(sellerUserId));
                
                if (!belongsToSeller) {
                        throw new RuntimeException("Order does not belong to this seller");
                }
                
                orderAccountingService.markCommissionAsPaid(order);
        }

        @Transactional
        public void payAllCommissions(Long sellerUserId) {
                List<Order> unpaidOrders = orderRepository.findOrdersBySeller(sellerUserId).stream()
                        .filter(o -> (o.getCommissionStatus() == com.example.jhapcham.order.CommissionStatus.UNPAID || 
                                     (o.getCommissionStatus() == com.example.jhapcham.order.CommissionStatus.PENDING && o.getStatus() == com.example.jhapcham.order.OrderStatus.DELIVERED)))
                        .toList();
                
                for (Order order : unpaidOrders) {
                        orderAccountingService.markCommissionAsPaid(order);
                }
        }
}
