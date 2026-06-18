package com.example.jhapcham.admin.application;

import com.example.jhapcham.admin.dto.DashboardStatisticsDTO;
import com.example.jhapcham.order.persistence.OrderRepository;
import com.example.jhapcham.product.persistence.ProductRepository;
import com.example.jhapcham.review.persistence.ReviewRepository;
import com.example.jhapcham.user.persistence.UserRepository;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.wishlist.persistence.WishlistRepository;
import com.example.jhapcham.analytics.persistence.PopularSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardStatisticsService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistRepository wishlistRepository;
    private final PopularSearchRepository popularSearchRepository;

    @Cacheable(cacheNames = "dashboard-statistics", key = "'all'")
    public DashboardStatisticsDTO getDashboardStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        
        Long totalUsers = userRepository.count();
        Long totalSellers = userRepository.countByRole(Role.SELLER);
        Long totalProducts = productRepository.count();
        Long totalOrders = orderRepository.count();
        
        // Calculate revenue and other metrics
        BigDecimal totalRevenue = calculateTotalRevenue();
        BigDecimal averageOrderValue = calculateAverageOrderValue();
        Long activeListings = productRepository.count(); // Simplified
        Long pendingOrders = (long) orderRepository.findByStatusOrderByCreatedAtDesc(com.example.jhapcham.order.domain.OrderStatus.PENDING).size();
        Long completedOrders = (long) orderRepository.findByStatusOrderByCreatedAtDesc(com.example.jhapcham.order.domain.OrderStatus.DELIVERED).size();
        Long cancelledOrders = (long) orderRepository.findByStatusOrderByCreatedAtDesc(com.example.jhapcham.order.domain.OrderStatus.CANCELLED).size();
        
        Long totalReviews = reviewRepository.count();
        Double averageProductRating = calculateAverageProductRating();
        Long totalWishlists = wishlistRepository.count();
        Long totalSearches = calculateTotalSearches();
        Double conversionRate = calculateConversionRate();
        
        Long newUsersThisMonth = (long) userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(monthStart))
                .count();
        Long newOrdersThisMonth = (long) orderRepository.findByCreatedAtAfter(monthStart).size();
        BigDecimal revenueThisMonth = calculateRevenueAfter(monthStart);
        
        return DashboardStatisticsDTO.builder()
                .totalUsers(totalUsers)
                .totalSellers(totalSellers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue.longValue())
                .averageOrderValue(averageOrderValue)
                .activeListings(activeListings)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .totalReviews(totalReviews)
                .averageProductRating(averageProductRating)
                .totalWishlists(totalWishlists)
                .totalSearches(totalSearches)
                .conversionRate(conversionRate)
                .newUsersThisMonth(newUsersThisMonth)
                .newOrdersThisMonth(newOrdersThisMonth)
                .revenueThisMonth(revenueThisMonth)
                .lastUpdated(now)
                .build();
    }

    private BigDecimal calculateTotalRevenue() {
        // This would need to be implemented based on your order/payment structure
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateAverageOrderValue() {
        // This would need to be implemented based on your order structure
        return BigDecimal.ZERO;
    }

    private Double calculateAverageProductRating() {
        // This would need to be implemented based on your review structure
        return 0.0;
    }

    private Long calculateTotalSearches() {
        return popularSearchRepository.count();
    }

    private Double calculateConversionRate() {
        // This would need to be implemented based on your analytics
        return 0.0;
    }

    private BigDecimal calculateRevenueAfter(LocalDateTime dateTime) {
        // This would need to be implemented based on your order/payment structure
        return BigDecimal.ZERO;
    }
}
