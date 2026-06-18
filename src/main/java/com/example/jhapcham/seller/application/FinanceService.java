package com.example.jhapcham.seller.application;


import com.example.jhapcham.seller.application.*;
import com.example.jhapcham.seller.domain.*;
import com.example.jhapcham.seller.dto.*;
import com.example.jhapcham.seller.persistence.*;
import com.example.jhapcham.order.domain.Order;
import com.example.jhapcham.order.domain.OrderItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class FinanceService {

    public BigDecimal calculateGrossSales(List<Order> orders) {
        return orders.stream()
                .map(Order::getItemsTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTotalCost(List<Order> orders, Long sellerUserId) {
        return orders.stream()
                .flatMap(order -> order.getItems().stream())
                .filter(item -> isSellerItem(item, sellerUserId))
                .map(item -> item.getBuyingLineTotalSnapshot() != null ? item.getBuyingLineTotalSnapshot() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateNetRevenue(BigDecimal grossSales, BigDecimal totalCommission, BigDecimal totalVat, BigDecimal totalPromo) {
        // Net Revenue = Gross Sales - Commission - VAT - Promos (absorbed by seller)
        BigDecimal net = grossSales.subtract(totalCommission).subtract(totalVat).subtract(totalPromo);
        return net.compareTo(BigDecimal.ZERO) > 0 ? net : BigDecimal.ZERO;
    }

    public BigDecimal calculateNetProfit(BigDecimal netRevenue, BigDecimal totalCost) {
        return netRevenue.subtract(totalCost);
    }

    public BigDecimal calculateProfitMargin(BigDecimal netProfit, BigDecimal grossSales) {
        if (grossSales.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return netProfit.divide(grossSales, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateAverageOrderProfit(BigDecimal netProfit, long totalOrders) {
        if (totalOrders == 0) return BigDecimal.ZERO;
        return netProfit.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
    }

    private boolean isSellerItem(OrderItem item, Long sellerUserId) {
        return item.getProduct() != null &&
               item.getProduct().getSellerProfile() != null &&
               item.getProduct().getSellerProfile().getUser() != null &&
               item.getProduct().getSellerProfile().getUser().getId().equals(sellerUserId);
    }
}
