package com.example.jhapcham.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformAnalyticsDTO {
    private long totalUsers;
    private long totalSellers;
    private long totalProducts;
    private long totalOrders;
    private double totalRevenue;
    private long pendingApplications;
    private long openReports;
    private long totalReviews;

    // Trend data for charts
    private java.util.Map<String, Long> dailyOrders;
    private java.util.Map<String, Double> monthlyRevenue;
}
