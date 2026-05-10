package com.example.jhapcham.promocode;

import com.example.jhapcham.campaign.DiscountType;
import com.example.jhapcham.order.CheckoutItemDTO;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductVariant;
import com.example.jhapcham.product.ProductVariantRepository;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    @Transactional
    public PromoCodeDTO createPromoCode(PromoCodeDTO dto) {
        validatePromoRequest(dto);
        String normalizedCode = dto.getCode().trim().toUpperCase();
        if (promoCodeRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Promo code already exists: " + normalizedCode);
        }

        PromoCode promo = PromoCode.builder()
                .code(normalizedCode)
                .sellerId(dto.getSellerId())
                .discountType(dto.getDiscountType())
                .discountValue(dto.getDiscountValue())
                .minOrderValue(dto.getMinOrderValue())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .usageLimit(dto.getUsageLimit())
                .perUserUsageLimit(dto.getPerUserUsageLimit() != null ? dto.getPerUserUsageLimit() : 1)
                .usedCount(0)
                .isActive(true)
                .scope(dto.getSellerId() != null ? PromoCode.PromoScope.SELLER_ONLY : PromoCode.PromoScope.GLOBAL)
                .build();

        PromoCode saved = promoCodeRepository.save(promo);
        dto.setId(saved.getId());
        return dto;
    }

    private void validatePromoRequest(PromoCodeDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Promo code details are required");
        }
        if (dto.getCode() == null || dto.getCode().trim().length() < 3 || dto.getCode().trim().length() > 40) {
            throw new IllegalArgumentException("Promo code must be 3 to 40 characters long");
        }
        if (!dto.getCode().trim().matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Promo code may contain only letters, numbers, underscores, and hyphens");
        }
        if (dto.getDiscountType() == null) {
            throw new IllegalArgumentException("Discount type is required");
        }
        if (dto.getDiscountValue() == null || dto.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount value must be greater than zero");
        }
        if (dto.getDiscountType() == DiscountType.PERCENTAGE
                && dto.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100");
        }
        if (dto.getMinOrderValue() == null || dto.getMinOrderValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum order value cannot be negative");
        }
        if (dto.getStartDate() == null || dto.getEndDate() == null || !dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new IllegalArgumentException("Promo start and end dates are invalid");
        }
        if (dto.getUsageLimit() == null || dto.getUsageLimit() <= 0) {
            throw new IllegalArgumentException("Usage limit must be greater than zero");
        }
        if (dto.getPerUserUsageLimit() != null && dto.getPerUserUsageLimit() <= 0) {
            throw new IllegalArgumentException("Per-user usage limit must be greater than zero");
        }
        if (dto.getPerUserUsageLimit() != null && dto.getPerUserUsageLimit() > dto.getUsageLimit()) {
            throw new IllegalArgumentException("Per-user usage limit cannot exceed total usage limit");
        }
    }

    public List<PromoCode> getSellerPromos(Long sellerId) {
        return promoCodeRepository.findBySellerId(sellerId);
    }

    @Transactional(readOnly = true)
    public DiscountResult calculateDiscount(String code, List<CheckoutItemDTO> items) {
        return calculateDiscount(code, items, null);
    }

    @Transactional(readOnly = true)
    public DiscountResult calculateDiscount(String code, List<CheckoutItemDTO> items, Long userId) {
        if (code == null || code.trim().isEmpty()) {
            return new DiscountResult(BigDecimal.ZERO, null);
        }

        String normalizedCode = code.trim().toUpperCase();
        PromoCode promo = promoCodeRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid promo code"));

        if (!promo.isValid()) {
            throw new IllegalArgumentException("Promo code is expired or inactive");
        }
        validatePerUserLimit(promo, userId);

        BigDecimal eligibleTotal = BigDecimal.ZERO;
        Long targetSellerId = (promo.getScope() == PromoCode.PromoScope.SELLER_ONLY) ? promo.getSellerId() : null;

        for (CheckoutItemDTO item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProductId()));

            if (promo.getScope() == PromoCode.PromoScope.SELLER_ONLY) {
                if (!product.getSellerProfile().getUser().getId().equals(promo.getSellerId())) {
                    continue;
                }
            }

            BigDecimal lineTotal = resolveLineTotal(product, item);
            eligibleTotal = eligibleTotal.add(lineTotal);
        }

        if (eligibleTotal.compareTo(BigDecimal.ZERO) == 0) {
            return new DiscountResult(BigDecimal.ZERO, targetSellerId);
        }

        if (eligibleTotal.compareTo(promo.getMinOrderValue()) < 0) {
            throw new IllegalArgumentException(
                    "Minimum order value of " + promo.getMinOrderValue() + " for eligible items not met.");
        }

        BigDecimal discountAmount;
        if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = eligibleTotal.multiply(promo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        } else {
            discountAmount = promo.getDiscountValue();
            if (discountAmount.compareTo(eligibleTotal) > 0) {
                discountAmount = eligibleTotal;
            }
        }

        return new DiscountResult(discountAmount, targetSellerId);
    }

    private BigDecimal resolveLineTotal(Product product, CheckoutItemDTO item) {
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        boolean hasVariants = Boolean.TRUE.equals(product.getHasVariants())
                || (product.getVariants() != null && product.getVariants().stream()
                        .anyMatch(variant -> Boolean.TRUE.equals(variant.getActive())));

        ProductVariant variant = null;
        if (item.getVariantId() != null) {
            variant = productVariantRepository.findById(item.getVariantId()).orElse(null);
            if (variant == null) {
                throw new IllegalArgumentException("Selected variant not found: " + item.getVariantId());
            }
            if (variant.getProduct() == null || !variant.getProduct().getId().equals(product.getId())) {
                throw new IllegalArgumentException("Selected variant does not belong to product: " + product.getId());
            }
            if (!Boolean.TRUE.equals(variant.getActive())) {
                throw new IllegalArgumentException("Selected variant is not available");
            }
        } else if (hasVariants) {
            throw new IllegalArgumentException("A variant must be selected for product: " + product.getName());
        }

        BigDecimal basePrice = (Boolean.TRUE.equals(product.getOnSale()) && product.getSalePrice() != null)
                ? product.getSalePrice()
                : product.getPrice();
        BigDecimal itemPrice = variant != null ? variant.getEffectivePrice(basePrice) : basePrice;
        return itemPrice.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Transactional
    public void incrementUsage(String code) {
        incrementUsage(code, null);
    }

    @Transactional
    public void incrementUsage(String code, Long userId) {
        String upperCode = code.trim().toUpperCase();
        PromoCode promo = promoCodeRepository.findByCodeForUpdate(upperCode)
                .orElse(null);

        if (promo != null) {
            validatePerUserLimitForUpdate(promo, userId);
            int newUsedCount = promo.getUsedCount() + 1;
            promo.setUsedCount(newUsedCount);
            if (newUsedCount >= promo.getUsageLimit()) {
                promo.setIsActive(false);
            }
            promoCodeRepository.save(promo);
        }
    }

    private void validatePerUserLimit(PromoCode promo, Long userId) {
        if (userId == null) {
            return;
        }
        int limit = promo.getPerUserUsageLimit() != null ? promo.getPerUserUsageLimit() : 1;
        promoCodeUsageRepository.findByPromoCodeAndUser_Id(promo, userId).ifPresent(usage -> {
            if (usage.getUsedCount() >= limit) {
                throw new IllegalArgumentException("Promo code usage limit reached for this user");
            }
        });
    }

    private void validatePerUserLimitForUpdate(PromoCode promo, Long userId) {
        if (userId == null) {
            return;
        }
        int limit = promo.getPerUserUsageLimit() != null ? promo.getPerUserUsageLimit() : 1;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        PromoCodeUsage usage = promoCodeUsageRepository.findByPromoCodeAndUserIdForUpdate(promo, userId)
                .orElse(PromoCodeUsage.builder()
                        .promoCode(promo)
                        .user(user)
                        .usedCount(0)
                        .build());
        if (usage.getUsedCount() >= limit) {
            throw new IllegalArgumentException("Promo code usage limit reached for this user");
        }
        usage.setUsedCount(usage.getUsedCount() + 1);
        usage.setLastUsedAt(java.time.LocalDateTime.now());
        promoCodeUsageRepository.save(usage);
    }

    @lombok.Value
    public static class DiscountResult {
        BigDecimal amount;
        Long applicableSellerId;
    }
}
