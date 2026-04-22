package com.example.jhapcham.order;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.seller.SellerProfile;
import com.example.jhapcham.seller.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAccountingService {

    private final SellerProfileRepository sellerProfileRepository;

    public BigDecimal calculateShippingFee(List<Product> products, BigDecimal itemsTotal, String shippingLocation) {
        BigDecimal maxShipping = BigDecimal.ZERO;

        for (Product p : products) {
            if (Boolean.TRUE.equals(p.getFreeShipping())) {
                continue;
            }

            if (p.getSellerFreeShippingMinOrder() != null 
                    && p.getSellerFreeShippingMinOrder() > 0.01
                    && itemsTotal.compareTo(BigDecimal.valueOf(p.getSellerFreeShippingMinOrder())) >= 0) {
                continue;
            }

            BigDecimal fee = BigDecimal.ZERO;
            if ("OUTSIDE".equalsIgnoreCase(shippingLocation)) {
                if (p.getOutsideValleyShipping() != null) {
                    fee = BigDecimal.valueOf(p.getOutsideValleyShipping());
                }
            } else {
                if (p.getInsideValleyShipping() != null) {
                    fee = BigDecimal.valueOf(p.getInsideValleyShipping());
                }
            }

            if (fee.compareTo(maxShipping) > 0) {
                maxShipping = fee;
            }
        }
        return maxShipping;
    }

    @Transactional
    public void initializeAccounting(Order order) {
        if (order.getItems().isEmpty()) {
            log.warn("Order {} has no items, skipping accounting init", order.getId());
            return;
        }

        SellerProfile seller = order.getItems().get(0).getProduct().getSellerProfile();

        // Gross items total
        BigDecimal itemsGross = order.getItemsTotal();
        // Discount given by seller via promo code
        BigDecimal discount = order.getDiscountTotal() != null ? order.getDiscountTotal() : BigDecimal.ZERO;

        // The gross amount the seller is entitled to from items is itemsGross - discount
        BigDecimal sellerGrossAmount = itemsGross.subtract(discount);

        // --- Commission Calculation ---
        BigDecimal totalCommission = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            BigDecimal itemLineTotal = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
            String category = item.getProduct() != null ? item.getProduct().getCategory() : "Others";
            double rate = getCommissionRate(category);
            BigDecimal itemCommission = itemLineTotal.multiply(BigDecimal.valueOf(rate / 100.0))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            
            item.setCommissionPercentageSnapshot(rate);
            item.setCommissionAmountSnapshot(itemCommission);
            totalCommission = totalCommission.add(itemCommission);
        }
        // Round commission to 2 decimal places
        totalCommission = totalCommission.setScale(2, java.math.RoundingMode.HALF_UP);
        // ------------------------------

        BigDecimal shippingFeePaidByCustomer = order.getShippingFee() != null ? order.getShippingFee()
                : BigDecimal.ZERO;

        BigDecimal sellerShippingCharge;
        if (shippingFeePaidByCustomer.compareTo(BigDecimal.ZERO) > 0) {
            sellerShippingCharge = BigDecimal.ZERO;
        } else {
            sellerShippingCharge = estimateSellerShippingCost(order, seller);
        }

        // Net = Gross - ShippingCharge - MarketplaceCommission
        BigDecimal net = sellerGrossAmount.subtract(sellerShippingCharge).subtract(totalCommission);

        order.setSellerGrossAmount(sellerGrossAmount);
        order.setSellerShippingCharge(sellerShippingCharge);
        order.setMarketplaceCommission(totalCommission);
        order.setSellerNetAmount(net);
        order.setCommissionStatus(CommissionStatus.PENDING);
        
        // Note: sellerAccounted remains false, SellerProfile amounts are NOT updated yet.
        log.info("Initialized accounting for order {}: Gross={}, ShippingCharge={}, Commission={}, Net={}",
                order.getId(), sellerGrossAmount, sellerShippingCharge, totalCommission, net);
    }

    @Transactional
    public void finalizeSellerAccounting(Order order) {
        if (order.isSellerAccounted()) {
            return;
        }

        if (order.getItems().isEmpty()) {
            log.warn("Order {} has no items, skipping summarize", order.getId());
            return;
        }

        // Only UNPAID commission if it's currently PENDING.
        if (order.getCommissionStatus() != CommissionStatus.PENDING) {
             log.warn("Order {} commission status is {}, cannot finalize", order.getId(), order.getCommissionStatus());
        }

        SellerProfile seller = order.getItems().get(0).getProduct().getSellerProfile();

        order.setSellerAccounted(true);
        order.setCommissionStatus(CommissionStatus.UNPAID);

        seller.setTotalIncome(seller.getTotalIncome().add(order.getSellerGrossAmount()));
        seller.setTotalShippingCost(seller.getTotalShippingCost().add(order.getSellerShippingCharge()));
        seller.setTotalCommission(seller.getTotalCommission().add(order.getMarketplaceCommission()));
        seller.setNetIncome(seller.getNetIncome().add(order.getSellerNetAmount()));

        sellerProfileRepository.save(seller);
        log.info("Finalized accounting for order {}: Status now UNPAID", order.getId());
    }

    @Transactional
    public void markCommissionAsPaid(Order order) {
        if (order.getCommissionStatus() != CommissionStatus.UNPAID && order.getCommissionStatus() != CommissionStatus.PENDING) {
            throw new RuntimeException("Cannot pay commission: current status is " + order.getCommissionStatus());
        }

        // Finalize the fine amount at the time of payment (re-calculate to ensure accuracy)
        BigDecimal fine = BigDecimal.ZERO;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (order.getCommissionDueDate() != null && now.isAfter(order.getCommissionDueDate())) {
            long daysLate = java.time.Duration.between(order.getCommissionDueDate(), now).toDays();
            long weeksLate = daysLate / 7;
            
            // Progressive fine: 10% (1st week) + 5% * additional weeks
            double multiplier = 0.10 + (weeksLate * 0.05);
            fine = order.getMarketplaceCommission().multiply(BigDecimal.valueOf(multiplier))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }

        order.setCommissionFineAmount(fine);
        order.setCommissionStatus(CommissionStatus.PAID);
        log.info("Commission for order {} has been marked as PAID. Penalty collected: Rs. {}", order.getId(), fine);
    }

    @Transactional
    public void cancelAccounting(Order order) {
        if (order.isSellerAccounted()) {
             // If already accounted, we might need to deduct from SellerProfile.
             // But usually you can't cancel a DELIVERED order directly. 
             // Just in case:
             log.warn("Order {} was already finalized, manual reversal required for SellerProfile.", order.getId());
        }
        
        order.setCommissionStatus(CommissionStatus.CANCELLED);
        order.setMarketplaceCommission(BigDecimal.ZERO);
        
        for (OrderItem item : order.getItems()) {
             item.setCommissionAmountSnapshot(BigDecimal.ZERO);
        }
        
        log.info("Cancelled accounting for order {}: Status now CANCELLED", order.getId());
    }

    public double getCommissionRate(String category) {
        if (category == null) return 15.0; // Others + 5% default
        String cat = category.toUpperCase().trim();

        if (cat.contains("ELECTRONICS") || cat.contains("GADGETS")) return 7.5;
        if (cat.contains("COMPUTER") || cat.contains("GAMING")) return 6.5;
        if (cat.contains("FASHION") || cat.contains("APPAREL")) return 20.0;
        if (cat.contains("FOOTWEAR")) return 16.0;
        if (cat.contains("ACCESSORIES")) return 20.0;
        if (cat.contains("JEWELRY") || cat.contains("LUXURY")) return 15.0;
        if (cat.contains("BEAUTY") || cat.contains("PERSONAL")) return 20.0;
        if (cat.contains("HOME") || cat.contains("LIVING")) return 14.0;
        if (cat.contains("SPORTS") || cat.contains("FITNESS")) return 12.5;
        if (cat.contains("BAGS") || cat.contains("TRAVEL")) return 15.0;
        if (cat.contains("BOOKS") || cat.contains("STATIONERY")) return 7.5;
        if (cat.contains("TOYS") || cat.contains("KIDS")) return 14.0;
        if (cat.contains("AUTOMOTIVE")) return 8.5;
        if (cat.contains("GROCERIES") || cat.contains("ESSENTIALS")) return 3.5;
        if (cat.contains("HEALTH") || cat.contains("WELLNESS")) return 15.0;

        return 15.0; // Default: Others base (~10%) + 5% extra as requested
    }

    private BigDecimal estimateSellerShippingCost(Order order, SellerProfile seller) {
        // If it's free shipping for the customer, the seller typically pays their standard delivery fee
        if ("OUTSIDE".equalsIgnoreCase(order.getShippingLocation())) {
            return BigDecimal
                    .valueOf(seller.getOutsideValleyDeliveryFee() != null ? seller.getOutsideValleyDeliveryFee() : 0);
        } else {
            return BigDecimal
                    .valueOf(seller.getInsideValleyDeliveryFee() != null ? seller.getInsideValleyDeliveryFee() : 0);
        }
    }
}
