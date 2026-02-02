package com.example.jhapcham.promocode;

import com.example.jhapcham.campaign.DiscountType;
import com.example.jhapcham.order.CheckoutItemDTO;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final ProductRepository productRepository; // A repository to check product and seller ownership

    @Transactional
    public PromoCodeDTO createPromoCode(PromoCodeDTO dto) {
        if (promoCodeRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Promo code already exists: " + dto.getCode());
        }

        PromoCode promo = PromoCode.builder()
                .code(dto.getCode().toUpperCase())
                .sellerId(dto.getSellerId())
                .discountType(dto.getDiscountType())
                .discountValue(dto.getDiscountValue())
                .minOrderValue(dto.getMinOrderValue())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .usageLimit(dto.getUsageLimit())
                .usedCount(0)
                .isActive(true)
                .scope(dto.getSellerId() != null ? PromoCode.PromoScope.SELLER_ONLY : PromoCode.PromoScope.GLOBAL)
                .build();

        PromoCode saved = promoCodeRepository.save(promo);
        dto.setId(saved.getId());
        return dto;
    }

    public List<PromoCode> getSellerPromos(Long sellerId) {
        return promoCodeRepository.findBySellerId(sellerId);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(String code, List<CheckoutItemDTO> items) {
        if (code == null || code.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        PromoCode promo = promoCodeRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid promo code"));

        if (!promo.isValid()) {
            throw new IllegalArgumentException("Promo code is expired or inactive");
        }

        // Calculate the eligible total for the promo code
        BigDecimal eligibleTotal = BigDecimal.ZERO;

        for (CheckoutItemDTO item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProductId()));

            // Check the scope: SELLER_ONLY
            if (promo.getScope() == PromoCode.PromoScope.SELLER_ONLY) {
                // Fixed: Access the seller ID via the seller profile relationship
                if (!product.getSellerProfile().getUser().getId().equals(promo.getSellerId())) {
                    continue; // Skip items not from this seller
                }
            }

            // Use the effective price (sale price if applicable)
            BigDecimal itemPrice = (Boolean.TRUE.equals(product.getOnSale()) && product.getSalePrice() != null)
                    ? product.getSalePrice()
                    : product.getPrice();

            BigDecimal lineTotal = itemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            eligibleTotal = eligibleTotal.add(lineTotal);
        }

        // If no items were eligible for this promo, return zero discount for this set
        // of items
        if (eligibleTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Perform a minimum order value check on the eligible amount
        if (eligibleTotal.compareTo(promo.getMinOrderValue()) < 0) {
            throw new IllegalArgumentException(
                    "Minimum order value of " + promo.getMinOrderValue() + " for eligible items not met.");
        }

        // Calculate the final discount amount
        BigDecimal discountAmount;
        if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
            // Fixed: Added the required RoundingMode for division
            discountAmount = eligibleTotal.multiply(promo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        } else {
            discountAmount = promo.getDiscountValue();
            // Cap the fixed discount at the eligible total
            if (discountAmount.compareTo(eligibleTotal) > 0) {
                discountAmount = eligibleTotal;
            }
        }

        return discountAmount;
    }

    @Transactional
    public void incrementUsage(String code) {
        PromoCode promo = promoCodeRepository.findByCode(code.toUpperCase())
                .orElse(null);
        if (promo != null) {
            promo.setUsedCount(promo.getUsedCount() + 1);
            if (promo.getUsedCount() >= promo.getUsageLimit()) {
                promo.setIsActive(false); // Auto deactivate
            }
            promoCodeRepository.save(promo);
        }
    }
}
