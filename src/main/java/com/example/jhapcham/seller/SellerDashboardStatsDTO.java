package com.example.jhapcham.seller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerDashboardStatsDTO {
    // Income metrics
    private BigDecimal totalIncome;
    private BigDecimal totalShippingCost;
    private BigDecimal netIncome;

    // Order counts
    private Long totalOrders;
    private Long deliveredOrders;
    private Long pendingOrders;
    private Long processingOrders;
    private Long shippedOrders;
    private Long canceledOrders;

    // Product metrics
    private Long totalProducts;
    private Long activeProducts;
    private Long inactiveProducts;

    // Recent activity
    private BigDecimal last30DaysIncome;
    private Long last30DaysOrders;

    // Store Branding
    private String storeName;
    private String logoImagePath;
    private String profileImagePath;

    // Dynamic Dashboard Data
    private List<BigDecimal> weeklySales;
    private List<ProductSummaryDTO> topSellingProducts;
}
