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
    public void applySellerAccounting(Order order) {
        if (order.isSellerAccounted()) {
            return;
        }

        if (order.getItems().isEmpty()) {
            log.warn("Order {} has no items, skipping accounting", order.getId());
            return;
        }

        SellerProfile seller = order.getItems().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Order has no items"))
                .getProduct()
                .getSellerProfile();

        BigDecimal gross = order.getItemsTotal();
        BigDecimal shippingFeePaidByCustomer = order.getShippingFee() != null ? order.getShippingFee()
                : BigDecimal.ZERO;

        // NEW LOGIC:
        // If shippingFeePaidByCustomer > 0, it means customer paid shipping.
        // The seller gets the full gross (items total).
        // If shippingFeePaidByCustomer == 0, it means it was FREE SHIPPING for
        // customer.
        // In that case, the seller covers the cost.

        BigDecimal sellerShippingCharge;
        if (shippingFeePaidByCustomer.compareTo(BigDecimal.ZERO) > 0) {
            sellerShippingCharge = BigDecimal.ZERO;
        } else {
            // Estimate shipping cost the seller has to pay (we use the same fee logic for
            // estimation here)
            // For now, let's assume if it was free for customer, seller pays their standard
            // shipping fee
            sellerShippingCharge = estimateSellerShippingCost(order, seller);
        }

        BigDecimal net = gross.subtract(sellerShippingCharge);

        order.setSellerGrossAmount(gross);
        order.setSellerShippingCharge(sellerShippingCharge);
        order.setSellerNetAmount(net);
        order.setSellerAccounted(true);

        seller.setTotalIncome(seller.getTotalIncome().add(gross));
        seller.setTotalShippingCost(seller.getTotalShippingCost().add(sellerShippingCharge));
        seller.setNetIncome(seller.getNetIncome().add(net));

        sellerProfileRepository.save(seller);
        log.info("Applied accounting for order {}: Gross={}, ShippingCharge={}, Net={}",
                order.getId(), gross, sellerShippingCharge, net);
    }

    private BigDecimal estimateSellerShippingCost(Order order, SellerProfile seller) {
        // If it's free shipping for the customer, the seller typically pays their
        // standard delivery fee
        if ("OUTSIDE".equalsIgnoreCase(order.getShippingLocation())) {
            return BigDecimal
                    .valueOf(seller.getOutsideValleyDeliveryFee() != null ? seller.getOutsideValleyDeliveryFee() : 0);
        } else {
            return BigDecimal
                    .valueOf(seller.getInsideValleyDeliveryFee() != null ? seller.getInsideValleyDeliveryFee() : 0);
        }
    }
}
