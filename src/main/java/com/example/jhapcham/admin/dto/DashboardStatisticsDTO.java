package com.example.jhapcham.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatisticsDTO {
    private Long totalUsers;
    private Long totalSellers;
    private Long totalProducts;
    private Long totalOrders;
    private Long totalRevenue;
    private BigDecimal averageOrderValue;
    private Long activeListings;
    private Long pendingOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private Long totalReviews;
    private Double averageProductRating;
    private Long totalWishlists;
    private Long totalSearches;
    private Double conversionRate;
    private Long newUsersThisMonth;
    private Long newOrdersThisMonth;
    private BigDecimal revenueThisMonth;
    private java.time.LocalDateTime lastUpdated;
}
