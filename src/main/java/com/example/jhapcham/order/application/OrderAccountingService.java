package com.example.jhapcham.order.application;

import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.seller.persistence.SellerProfileRepository;
import com.example.jhapcham.order.persistence.OrderRepository;
import com.example.jhapcham.product.domain.Product;
import com.example.jhapcham.seller.domain.SellerProfile;
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
    private final OrderRepository orderRepository;

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

        BigDecimal itemsGross = money(order.getItemsTotal());
        BigDecimal sellerPromoDiscount = money(order.getSellerPromoDiscountAmount());
        BigDecimal platformSponsoredDiscount = money(order.getPlatformSponsoredDiscountAmount());
        BigDecimal totalDiscount = money(order.getDiscountTotal());

        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal inputVatTotal = BigDecimal.ZERO;
        BigDecimal outputVatTotal = BigDecimal.ZERO;
        BigDecimal vatPayableTotal = BigDecimal.ZERO;
        BigDecimal grossProfitTotal = BigDecimal.ZERO;
        BigDecimal netProfitTotal = BigDecimal.ZERO;
        BigDecimal finalSellerEarningsTotal = BigDecimal.ZERO;

        BigDecimal sellerGrossAmount = itemsGross.subtract(sellerPromoDiscount).setScale(2, java.math.RoundingMode.HALF_UP);
        for (OrderItem item : order.getItems()) {
            BigDecimal itemLineTotal = money(item.getLineTotal());
            BigDecimal sellerPromoShare = proportional(sellerPromoDiscount, itemLineTotal, itemsGross);
            BigDecimal platformDiscountShare = proportional(platformSponsoredDiscount, itemLineTotal, itemsGross);
            BigDecimal totalDiscountShare = proportional(totalDiscount, itemLineTotal, itemsGross);
            BigDecimal commissionBase = itemLineTotal.subtract(totalDiscountShare);
            if (commissionBase.compareTo(BigDecimal.ZERO) < 0) {
                commissionBase = BigDecimal.ZERO;
            }

            String category = item.getProduct() != null ? item.getProduct().getCategory() : "Others";
            double rate = getCommissionRate(category);
            BigDecimal itemCommission = commissionBase.multiply(BigDecimal.valueOf(rate / 100.0))
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            BigDecimal buyingLineTotal = money(item.getBuyingLineTotalSnapshot());
            BigDecimal sellingLineTotal = money(item.getSellingLineTotalSnapshot() != null ? item.getSellingLineTotalSnapshot() : itemLineTotal);
            BigDecimal inputVat = calculateIncludedVat(buyingLineTotal);
            BigDecimal outputVat = calculateIncludedVat(commissionBase);
            BigDecimal vatPayable = outputVat.subtract(inputVat).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal grossProfit = sellingLineTotal.subtract(buyingLineTotal).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal netProfit = grossProfit.subtract(sellerPromoShare).subtract(vatPayable).subtract(itemCommission)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal finalSellerEarning = sellingLineTotal.subtract(sellerPromoShare).subtract(vatPayable).subtract(itemCommission)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            item.setCommissionPercentageSnapshot(rate);
            item.setCommissionAmountSnapshot(itemCommission);
            item.setInputVatAmountSnapshot(inputVat);
            item.setOutputVatAmountSnapshot(outputVat);
            item.setVatPayableSnapshot(vatPayable);
            item.setSellerPromoDiscountSnapshot(sellerPromoShare);
            item.setPlatformDiscountSnapshot(platformDiscountShare);
            item.setCommissionBaseSnapshot(commissionBase.setScale(2, java.math.RoundingMode.HALF_UP));
            item.setGrossProfitSnapshot(grossProfit);
            item.setNetProfitSnapshot(netProfit);
            item.setFinalSellerEarningSnapshot(finalSellerEarning);

            totalCommission = totalCommission.add(itemCommission);
            inputVatTotal = inputVatTotal.add(inputVat);
            outputVatTotal = outputVatTotal.add(outputVat);
            vatPayableTotal = vatPayableTotal.add(vatPayable);
            grossProfitTotal = grossProfitTotal.add(grossProfit);
            netProfitTotal = netProfitTotal.add(netProfit);
            finalSellerEarningsTotal = finalSellerEarningsTotal.add(finalSellerEarning);
        }
        totalCommission = totalCommission.setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal shippingFeePaidByCustomer = order.getShippingFee() != null ? order.getShippingFee()
                : BigDecimal.ZERO;

        BigDecimal sellerShippingCharge;
        if (shippingFeePaidByCustomer.compareTo(BigDecimal.ZERO) > 0) {
            sellerShippingCharge = BigDecimal.ZERO;
        } else {
            sellerShippingCharge = estimateSellerShippingCost(order, seller);
        }

        BigDecimal net = sellerGrossAmount.subtract(vatPayableTotal).subtract(sellerShippingCharge).subtract(totalCommission)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        order.setSellerGrossAmount(sellerGrossAmount);
        order.setSellerShippingCharge(sellerShippingCharge);
        order.setMarketplaceCommission(totalCommission);
        order.setSellerNetAmount(net);
        order.setInputVatAmount(inputVatTotal.setScale(2, java.math.RoundingMode.HALF_UP));
        order.setOutputVatAmount(outputVatTotal.setScale(2, java.math.RoundingMode.HALF_UP));
        order.setVatPayableAmount(vatPayableTotal.setScale(2, java.math.RoundingMode.HALF_UP));
        order.setGrossProfitAmount(grossProfitTotal.setScale(2, java.math.RoundingMode.HALF_UP));
        order.setNetProfitAmount(netProfitTotal.subtract(sellerShippingCharge).setScale(2, java.math.RoundingMode.HALF_UP));
        order.setFinalSellerEarnings(finalSellerEarningsTotal.subtract(sellerShippingCharge).setScale(2, java.math.RoundingMode.HALF_UP));
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
        seller.setTotalVatCollected(seller.getTotalVatCollected().add(order.getVatPayableAmount() != null ? order.getVatPayableAmount() : BigDecimal.ZERO));
        seller.setNetIncome(seller.getNetIncome().add(order.getSellerNetAmount()));

        sellerProfileRepository.save(seller);
        log.info("Finalized accounting for order {}: Status now UNPAID", order.getId());
    }

    @Transactional
    public void finalizeDeliveredOrder(Order order) {
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            if (order.getPaymentStatus() == PaymentStatus.COD_REMITTED) {
                finalizeSellerAccounting(order);
            }
            return;
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            finalizeSellerAccounting(order);
        }
    }

    @Transactional
    public void markCommissionAsPaid(Order order) {
        if (order.getCommissionStatus() == CommissionStatus.PAID) {
            log.info("Commission for order {} has already been paid.", order.getId());
            return;
        }
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
             SellerProfile seller = order.getItems().isEmpty() ? null : order.getItems().get(0).getProduct().getSellerProfile();
             if (seller != null) {
                 seller.setTotalIncome(safeSubtract(seller.getTotalIncome(), order.getSellerGrossAmount()));
                 seller.setTotalShippingCost(safeSubtract(seller.getTotalShippingCost(), order.getSellerShippingCharge()));
                 seller.setTotalCommission(safeSubtract(seller.getTotalCommission(), order.getMarketplaceCommission()));
                 seller.setTotalVatCollected(safeSubtract(seller.getTotalVatCollected(),
                         order.getVatPayableAmount() != null ? order.getVatPayableAmount() : BigDecimal.ZERO));
                 seller.setNetIncome(safeSubtract(seller.getNetIncome(), order.getSellerNetAmount()));
                 sellerProfileRepository.save(seller);
             }
             order.setSellerAccounted(false);
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

    private BigDecimal calculateIncludedVat(BigDecimal taxInclusiveAmount) {
        BigDecimal amount = money(taxInclusiveAmount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.subtract(amount.divide(new BigDecimal("1.13"), 2, java.math.RoundingMode.HALF_UP))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal proportional(BigDecimal total, BigDecimal part, BigDecimal whole) {
        total = money(total);
        part = money(part);
        whole = money(whole);
        if (total.compareTo(BigDecimal.ZERO) == 0 || whole.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return total.multiply(part).divide(whole, 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value != null ? value.setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private BigDecimal safeSubtract(BigDecimal current, BigDecimal amount) {
        BigDecimal left = current != null ? current : BigDecimal.ZERO;
        BigDecimal right = amount != null ? amount : BigDecimal.ZERO;
        return left.subtract(right);
    }

    private BigDecimal safeGet(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
