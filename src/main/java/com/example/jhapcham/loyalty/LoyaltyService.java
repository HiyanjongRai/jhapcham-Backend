package com.example.jhapcham.loyalty;

import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.notification.EmailService;
import com.example.jhapcham.notification.NotificationService;
import com.example.jhapcham.notification.NotificationType;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final LoyaltyPointsRepository loyaltyPointsRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    private static final Long POINTS_PER_ORDER = 10L;  // 1 point per Rs. 100 spent
    private static final Long SILVER_THRESHOLD = 500L;
    private static final Long GOLD_THRESHOLD = 2000L;
    private static final Long PLATINUM_THRESHOLD = 5000L;

    @Transactional
    public void initializeLoyaltyPoints(User user) {
        if (loyaltyPointsRepository.findByUser(user).isEmpty()) {
            LoyaltyPoints loyalty = LoyaltyPoints.builder()
                    .user(user)
                    .totalPoints(0L)
                    .redeemedPoints(0L)
                    .availablePoints(0L)
                    .build();
            loyaltyPointsRepository.save(loyalty);
            log.info("Loyalty points initialized for user: {}", user.getId());
        }
    }

    @Transactional
    public void addPointsForOrder(Long userId, Long orderTotal) {
        if (orderTotal == null || orderTotal <= 0) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LoyaltyPoints loyalty = loyaltyPointsRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found"));

        // Calculate points: 1 point per Rs. 100
        Long pointsToAdd = orderTotal / 100;
        if (pointsToAdd <= 0) {
            return;
        }

        loyalty.addPoints(pointsToAdd);
        loyaltyPointsRepository.save(loyalty);

        log.info("Added {} points to user {} for order totaling {}", pointsToAdd, userId, orderTotal);

        // Send notification
        notificationService.createNotification(user, "Loyalty Points Earned",
                "You earned " + pointsToAdd + " points! Total: " + loyalty.getTotalPoints(),
                NotificationType.LOYALTY_UPDATE, loyalty.getId());

        // Check for tier upgrade
        checkAndUpgradeTier(user, loyalty);
    }

    @Transactional
    public LoyaltyPointsDTO redeemPoints(Long userId, Long pointsToRedeem) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LoyaltyPoints loyalty = loyaltyPointsRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found"));

        loyalty.redeemPoints(pointsToRedeem);
        loyaltyPointsRepository.save(loyalty);

        log.info("User {} redeemed {} points", userId, pointsToRedeem);

        // Send notification
        notificationService.createNotification(user, "Loyalty Points Redeemed",
                "You redeemed " + pointsToRedeem + " points. Remaining: " + loyalty.getAvailablePoints(),
                NotificationType.LOYALTY_UPDATE, loyalty.getId());

        // Send email
        Long discountAmount = pointsToRedeem;  // 1 point = Rs. 1
        emailService.sendLoyaltyRedemptionEmail(user.getEmail(), user.getFullName(), pointsToRedeem, discountAmount);

        return toLoyaltyDTO(loyalty);
    }

    public LoyaltyPointsDTO getUserLoyaltyPoints(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LoyaltyPoints loyalty = loyaltyPointsRepository.findByUser(user)
                .orElseGet(() -> {
                    LoyaltyPoints newLoyalty = LoyaltyPoints.builder()
                            .user(user)
                            .totalPoints(0L)
                            .redeemedPoints(0L)
                            .availablePoints(0L)
                            .build();
                    return loyaltyPointsRepository.save(newLoyalty);
                });

        return toLoyaltyDTO(loyalty);
    }

    private void checkAndUpgradeTier(User user, LoyaltyPoints loyalty) {
        String currentTier = getTier(loyalty.getTotalPoints());
        String previousTier = getTier(loyalty.getTotalPoints() - 1);

        if (!currentTier.equals(previousTier)) {
            log.info("User {} upgraded to {} tier", user.getId(), currentTier);

            String tierBenefits = getTierBenefits(currentTier);
            notificationService.createNotification(user, "Tier Upgraded! 🎉",
                    "Congratulations! You've reached " + currentTier + " tier. " + tierBenefits,
                    NotificationType.LOYALTY_UPDATE, loyalty.getId());

            // Send email
            emailService.sendTierUpgradeEmail(user.getEmail(), user.getFullName(), currentTier, tierBenefits);
        }
    }

    private String getTier(Long points) {
        if (points >= PLATINUM_THRESHOLD) return "PLATINUM";
        if (points >= GOLD_THRESHOLD) return "GOLD";
        if (points >= SILVER_THRESHOLD) return "SILVER";
        return "BRONZE";
    }

    private String getTierBenefits(String tier) {
        return switch (tier) {
            case "SILVER" -> "Enjoy 5% discount on all purchases and exclusive deals!";
            case "GOLD" -> "Enjoy 10% discount on all purchases, free shipping, and VIP support!";
            case "PLATINUM" -> "Enjoy 15% discount on all purchases, free shipping, VIP support, and early access to sales!";
            default -> "Start earning points to unlock benefits!";
        };
    }

    private LoyaltyPointsDTO toLoyaltyDTO(LoyaltyPoints loyalty) {
        Long currentPoints = loyalty.getTotalPoints();
        String tier = getTier(currentPoints);
        String nextTier = getNextTier(tier);
        Long pointsToNextTier = getPointsToNextTier(tier);
        Long diff = pointsToNextTier - loyalty.getTotalPoints();

        return LoyaltyPointsDTO.builder()
                .id(loyalty.getId())
                .totalPoints(loyalty.getTotalPoints())
                .redeemedPoints(loyalty.getRedeemedPoints())
                .availablePoints(loyalty.getAvailablePoints())
                .lastRedeemedAt(loyalty.getLastRedeemedAt())
                .tier(tier)
                .nextTier(nextTier)
                .pointsToNextTier(diff < 0 ? 0L : diff)
                .build();
    }

    private String getNextTier(String currentTier) {
        return switch (currentTier) {
            case "BRONZE" -> "SILVER";
            case "SILVER" -> "GOLD";
            case "GOLD" -> "PLATINUM";
            default -> "PLATINUM";
        };
    }

    private Long getPointsToNextTier(String tier) {
        return switch (tier) {
            case "BRONZE" -> SILVER_THRESHOLD;
            case "SILVER" -> GOLD_THRESHOLD;
            case "GOLD" -> PLATINUM_THRESHOLD;
            default -> PLATINUM_THRESHOLD;
        };
    }
}
