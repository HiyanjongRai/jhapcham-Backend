package com.example.jhapcham.loyalty;

import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderItem;
import com.example.jhapcham.seller.SellerProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RewardCalculationService {
    private static final BigDecimal DEFAULT_BASE_RATE = new BigDecimal("0.01");

    private final RewardRuleRepository rewardRuleRepository;
    private final TierConfigRepository tierConfigRepository;

    public Long calculateEarnPoints(Order order, LoyaltyTier tier) {
        List<RewardRule> rules = rewardRuleRepository.findActiveRules(LocalDateTime.now());
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            BigDecimal eligible = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
            BigDecimal rate = calculateRateForItem(item, rules);
            total = total.add(eligible.multiply(rate));
        }
        BigDecimal tierMultiplier = tierConfigRepository.findByTier(tier)
                .map(TierConfig::getRewardMultiplier)
                .orElse(BigDecimal.ONE);
        return total.multiply(tierMultiplier).setScale(0, RoundingMode.DOWN).longValue();
    }

    private BigDecimal calculateRateForItem(OrderItem item, List<RewardRule> rules) {
        BigDecimal rate = rules.stream()
                .filter(r -> r.getRuleType() == RewardRuleType.BASE)
                .map(RewardRule::getRewardRate)
                .findFirst()
                .orElse(DEFAULT_BASE_RATE);

        String category = item.getProduct() != null ? item.getProduct().getCategory() : null;
        SellerProfile seller = item.getProduct() != null ? item.getProduct().getSellerProfile() : null;
        Long sellerId = seller != null ? seller.getId() : null;

        for (RewardRule rule : rules) {
            if (rule.getRuleType() == RewardRuleType.CATEGORY
                    && category != null
                    && rule.getCategory() != null
                    && category.equalsIgnoreCase(rule.getCategory())) {
                rate = rate.max(rule.getRewardRate());
            } else if (rule.getRuleType() == RewardRuleType.SELLER
                    && sellerId != null
                    && sellerId.equals(rule.getSellerId())) {
                rate = rate.add(rule.getRewardRate());
            } else if (rule.getRuleType() == RewardRuleType.SEASONAL) {
                rate = rate.add(rule.getRewardRate());
            } else if (rule.getRuleType() == RewardRuleType.BONUS_MULTIPLIER && rule.getMultiplier() != null) {
                rate = rate.multiply(rule.getMultiplier());
            }
        }
        return rate.max(BigDecimal.ZERO);
    }
}
