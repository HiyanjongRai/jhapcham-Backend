package com.example.jhapcham.loyalty.application;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.notification.application.NotificationService;
import com.example.jhapcham.notification.domain.NotificationType;
import com.example.jhapcham.order.domain.Order;
import com.example.jhapcham.order.persistence.OrderRepository;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.user.domain.User;
import com.example.jhapcham.user.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyService {
    private static final BigDecimal POINT_TO_RUPEE = BigDecimal.ONE;
    private static final BigDecimal MAX_REDEMPTION_PERCENT = new BigDecimal("0.30");
    private static final BigDecimal MIN_REDEMPTION_ORDER_AMOUNT = new BigDecimal("500.00");
    private static final int EXPIRY_MONTHS = 12;
    private static final int EXPIRY_NOTICE_DAYS = 14;
    private static final int RETURN_WINDOW_DAYS = 7;

    private final LoyaltyWalletRepository walletRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final RewardRuleRepository rewardRuleRepository;
    private final TierConfigRepository tierConfigRepository;
    private final RedemptionHistoryRepository redemptionHistoryRepository;
    private final ExpiryScheduleRepository expiryScheduleRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final RewardCalculationService rewardCalculationService;
    private final NotificationService notificationService;
    private final com.example.jhapcham.notification.application.EmailService emailService;

    @Transactional
    @CacheEvict(value = "loyaltyWallet", key = "#user.id")
    public void initializeLoyaltyPoints(User user) {
        if (user == null || user.getRole() != Role.CUSTOMER) {
            return;
        }
        walletRepository.findByCustomer(user).orElseGet(() -> walletRepository.save(LoyaltyWallet.builder()
                .customer(user)
                .tier(resolveTier(0L))
                .build()));
        ensureDefaultConfiguration();
    }

    @Transactional
    @Cacheable(value = "loyaltyWallet", key = "#userId")
    public LoyaltyWalletDTO getWallet(Long userId) {
        LoyaltyWallet wallet = walletRepository.findByCustomerId(userId)
                .orElseGet(() -> createWallet(userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"))));
        return toWalletDTO(wallet, PageRequestLite.firstFive());
    }

    @Transactional(readOnly = true)
    public LoyaltyPointsDTO getUserLoyaltyPoints(Long userId) {
        LoyaltyWalletDTO wallet = getWallet(userId);
        return LoyaltyPointsDTO.builder()
                .id(wallet.getId())
                .totalPoints(wallet.getTotalPointsEarned())
                .redeemedPoints(wallet.getRedeemedPoints())
                .availablePoints(wallet.getAvailablePoints())
                .lastRedeemedAt(wallet.getLastRedeemedAt())
                .tier(wallet.getTier().name())
                .nextTier(wallet.getNextTier() != null ? wallet.getNextTier().name() : wallet.getTier().name())
                .pointsToNextTier(wallet.getPointsToNextTier())
                .history(wallet.getRecentTransactions())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<LoyaltyTransactionDTO> getTransactions(Long userId, Pageable pageable) {
        return transactionRepository.findByCustomerIdOrderByCreatedAtDesc(userId, pageable).map(this::toTransactionDTO);
    }

    @Transactional(readOnly = true)
    public RedemptionQuoteDTO quoteRedemption(Long userId, BigDecimal orderTotal, Long requestedPoints) {
        LoyaltyWallet wallet = walletRepository.findByCustomerId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty wallet not found"));
        return quote(wallet, orderTotal, requestedPoints);
    }

    @Transactional
    @CacheEvict(value = "loyaltyWallet", key = "#userId")
    public LoyaltyPointsDTO redeemPoints(Long userId, Long pointsToRedeem) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        LoyaltyWallet wallet = walletRepository.findByCustomerIdForUpdate(userId)
                .orElseGet(() -> createWallet(user));
        if (wallet.isFrozen()) {
            throw new BusinessValidationException("Loyalty wallet is frozen");
        }
        validateRedeem(wallet, pointsToRedeem);
        wallet.setAvailablePoints(wallet.getAvailablePoints() - pointsToRedeem);
        wallet.setRedeemedPoints(wallet.getRedeemedPoints() + pointsToRedeem);
        wallet.setLastRedeemedAt(LocalDateTime.now());
        walletRepository.save(wallet);
        saveTransaction(user, null, LoyaltyTransactionType.REDEEM, pointsToRedeem,
                pointsToValue(pointsToRedeem), "Manual points redemption", LoyaltyTransactionStatus.AVAILABLE,
                "manual-redeem-" + userId + "-" + UUID.randomUUID(), null);
        notify(user, "Loyalty Points Redeemed", "You redeemed " + pointsToRedeem + " points.", null);
        return getUserLoyaltyPoints(userId);
    }

    @Transactional
    @CacheEvict(value = "loyaltyWallet", key = "#user.id")
    public RedemptionQuoteDTO commitCheckoutRedemption(User user, Order order, Long requestedPoints, BigDecimal orderTotal) {
        if (user == null || requestedPoints == null || requestedPoints <= 0) {
            return RedemptionQuoteDTO.builder().requestedPoints(0L).approvedPoints(0L).discountAmount(BigDecimal.ZERO).build();
        }
        LoyaltyWallet wallet = walletRepository.findByCustomerIdForUpdate(user.getId()).orElseGet(() -> createWallet(user));
        RedemptionQuoteDTO quote = quote(wallet, orderTotal, requestedPoints);
        if (quote.getApprovedPoints() <= 0) {
            return quote;
        }
        wallet.setAvailablePoints(wallet.getAvailablePoints() - quote.getApprovedPoints());
        wallet.setRedeemedPoints(wallet.getRedeemedPoints() + quote.getApprovedPoints());
        wallet.setLastRedeemedAt(LocalDateTime.now());
        walletRepository.save(wallet);
        redemptionHistoryRepository.save(RedemptionHistory.builder()
                .customer(user)
                .order(order)
                .pointsRedeemed(quote.getApprovedPoints())
                .discountAmount(quote.getDiscountAmount())
                .build());
        saveTransaction(user, order, LoyaltyTransactionType.REDEEM, quote.getApprovedPoints(),
                quote.getDiscountAmount(), "Redeemed at checkout for order #" + order.getId(),
                LoyaltyTransactionStatus.AVAILABLE, "redeem-order-" + order.getId(), null);
        notify(user, "Reward Applied", "You used " + quote.getApprovedPoints() + " points on order #" + order.getId() + ".", order.getId());
        return quote;
    }

    @Transactional
    public void recordPaymentCompleted(Long orderId) {
        Order order = getOrder(orderId);
        if (order.getUser() == null) {
            return;
        }
        ensureWallet(order.getUser());
        if (transactionRepository.existsByReferenceKey("pending-order-" + orderId)) {
            return;
        }
        Long pending = rewardCalculationService.calculateEarnPoints(order, getWalletEntity(order.getUser().getId()).getTier());
        if (pending <= 0) {
            return;
        }
        LoyaltyWallet wallet = walletRepository.findByCustomerIdForUpdate(order.getUser().getId())
                .orElseGet(() -> createWallet(order.getUser()));
        wallet.setPendingPoints(wallet.getPendingPoints() + pending);
        walletRepository.save(wallet);
        saveTransaction(order.getUser(), order, LoyaltyTransactionType.PENDING_REWARD, pending, null,
                "Pending reward locked until delivery for order #" + orderId,
                LoyaltyTransactionStatus.PENDING, "pending-order-" + orderId, null);
    }

    @Transactional
    @CacheEvict(value = "loyaltyWallet", key = "#customerId")
    public void earnForDeliveredOrder(Long orderId, Long customerId) {
        Order order = getOrder(orderId);
        if (order.getUser() == null) {
            return;
        }
        LoyaltyWallet wallet = walletRepository.findByCustomerIdForUpdate(order.getUser().getId())
                .orElseGet(() -> createWallet(order.getUser()));
        if (wallet.isFrozen()) {
            saveTransaction(order.getUser(), order, LoyaltyTransactionType.FRAUD_LOCK, 0L, null,
                    "Reward locked because wallet is frozen", LoyaltyTransactionStatus.FLAGGED,
                    "fraud-lock-order-" + orderId, null);
            return;
        }
        String ref = "earn-order-" + orderId;
        if (transactionRepository.existsByReferenceKey(ref)) {
            return;
        }
        Long points = rewardCalculationService.calculateEarnPoints(order, wallet.getTier());
        if (points <= 0) {
            return;
        }
        wallet.setAvailablePoints(wallet.getAvailablePoints() + points);
        wallet.setTotalPointsEarned(wallet.getTotalPointsEarned() + points);
        wallet.setLifetimePoints(wallet.getLifetimePoints() + points);
        wallet.setPendingPoints(Math.max(0L, wallet.getPendingPoints() - points));
        wallet.setLastEarnedAt(LocalDateTime.now());
        LoyaltyTier previous = wallet.getTier();
        wallet.setTier(resolveTier(wallet.getLifetimePoints()));
        if (previous != wallet.getTier()) {
            wallet.setTierUpdatedAt(LocalDateTime.now());
        }
        walletRepository.save(wallet);
        LoyaltyTransaction tx = saveTransaction(order.getUser(), order, LoyaltyTransactionType.EARN, points, null,
                "Earned rewards for delivered order #" + orderId, LoyaltyTransactionStatus.AVAILABLE, ref,
                "returnWindowEndsAt=" + LocalDateTime.now().plusDays(RETURN_WINDOW_DAYS));
        expiryScheduleRepository.save(ExpirySchedule.builder()
                .customer(order.getUser())
                .transaction(tx)
                .pointsRemaining(points)
                .expiresAt(LocalDateTime.now().plusMonths(EXPIRY_MONTHS))
                .build());
        notify(order.getUser(), "Points Earned", "You earned " + points + " points from order #" + orderId + ".", orderId);
        if (previous != wallet.getTier()) {
            notify(order.getUser(), "Tier Upgraded", "You are now a " + wallet.getTier() + " rewards member.", orderId);
            emailService.sendTierUpgradeEmail(order.getUser().getEmail(), order.getUser().getFullName(), wallet.getTier().name(), tierBenefits(wallet.getTier()));
        }
    }

    @Transactional
    @CacheEvict(value = "loyaltyWallet", key = "#customerId")
    public void reverseForRefundOrCancel(Long orderId, Long customerId, boolean refund) {
        reverseForRefundOrCancel(orderId, customerId, refund, null);
    }

    @Transactional
    @CacheEvict(value = "loyaltyWallet", key = "#customerId")
    public void reverseForRefundOrCancel(Long orderId, Long customerId, boolean refund, BigDecimal refundAmount) {
        reverseForRefundOrCancel(orderId, customerId, refund, refundAmount, refund ? "refund-order-" + orderId : "cancel-order-" + orderId);
    }

    @Transactional
    @CacheEvict(value = "loyaltyWallet", key = "#customerId")
    public void reverseForRefund(Long orderId, Long customerId, BigDecimal refundAmount, Long refundId) {
        reverseForRefundOrCancel(orderId, customerId, true, refundAmount, "refund-" + refundId);
    }

    private void reverseForRefundOrCancel(Long orderId, Long customerId, boolean refund, BigDecimal refundAmount, String reversalKey) {
        Order order = getOrder(orderId);
        if (order.getUser() == null) {
            return;
        }
        LoyaltyWallet wallet = walletRepository.findByCustomerIdForUpdate(order.getUser().getId())
                .orElseGet(() -> createWallet(order.getUser()));
        BigDecimal orderTotal = order.getGrandTotal() != null ? order.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal reversalRatio = refund && refundAmount != null && orderTotal.compareTo(BigDecimal.ZERO) > 0
                ? refundAmount.divide(orderTotal, 8, RoundingMode.HALF_UP).min(BigDecimal.ONE)
                : BigDecimal.ONE;
        for (LoyaltyTransaction earned : transactionRepository.findByOrderIdAndTransactionTypeIn(orderId,
                List.of(LoyaltyTransactionType.EARN, LoyaltyTransactionType.PENDING_REWARD))) {
            String reverseReference = "reverse-" + reversalKey + "-" + earned.getId();
            if (transactionRepository.existsByReferenceKey(reverseReference)) {
                continue;
            }
            if (earned.getStatus() == LoyaltyTransactionStatus.REVERSED || earned.getStatus() == LoyaltyTransactionStatus.CANCELLED) {
                continue;
            }
            Long target = BigDecimal.valueOf(Math.max(0L, earned.getPoints())).multiply(reversalRatio)
                    .setScale(0, RoundingMode.CEILING).longValue();
            Long remove = Math.min(wallet.getAvailablePoints(), target);
            wallet.setAvailablePoints(wallet.getAvailablePoints() - remove);
            wallet.setTotalPointsEarned(Math.max(0L, wallet.getTotalPointsEarned() - remove));
            if (reversalRatio.compareTo(BigDecimal.ONE) >= 0) {
                earned.setStatus(earned.getTransactionType() == LoyaltyTransactionType.PENDING_REWARD
                        ? LoyaltyTransactionStatus.CANCELLED : LoyaltyTransactionStatus.REVERSED);
                earned.setReversedAt(LocalDateTime.now());
                transactionRepository.save(earned);
            }
            saveTransaction(order.getUser(), order, LoyaltyTransactionType.REFUND_REVERSAL, -remove, null,
                    (refund ? "Refund" : "Cancellation") + " reward reversal for order #" + orderId,
                    LoyaltyTransactionStatus.REVERSED,
                    reverseReference, null);
        }
        restoreRedemptions(order, wallet);
        wallet.setTier(resolveTier(wallet.getLifetimePoints()));
        walletRepository.save(wallet);
    }

    @Transactional
    public void processExpiry() {
        LocalDateTime now = LocalDateTime.now();
        for (ExpirySchedule expiry : expiryScheduleRepository.findTop200ByExpiredFalseAndExpiresAtBeforeOrderByExpiresAtAsc(now)) {
            LoyaltyWallet wallet = walletRepository.findByCustomerIdForUpdate(expiry.getCustomer().getId())
                    .orElseGet(() -> createWallet(expiry.getCustomer()));
            Long points = Math.min(wallet.getAvailablePoints(), expiry.getPointsRemaining());
            if (points <= 0) {
                expiry.setExpired(true);
                expiryScheduleRepository.save(expiry);
                continue;
            }
            wallet.setAvailablePoints(wallet.getAvailablePoints() - points);
            wallet.setExpiredPoints(wallet.getExpiredPoints() + points);
            walletRepository.save(wallet);
            expiry.setExpired(true);
            expiry.setPointsRemaining(0L);
            expiryScheduleRepository.save(expiry);
            saveTransaction(expiry.getCustomer(), expiry.getTransaction().getOrder(), LoyaltyTransactionType.EXPIRE, -points, null,
                    "Points expired", LoyaltyTransactionStatus.EXPIRED, "expire-" + expiry.getId(), null);
            notify(expiry.getCustomer(), "Points Expired", points + " reward points expired.", null);
        }
    }

    @Transactional
    public void notifyExpiringSoon() {
        LocalDateTime now = LocalDateTime.now();
        for (ExpirySchedule expiry : expiryScheduleRepository.findTop200ByExpiredFalseAndNotifiedFalseAndExpiresAtBetweenOrderByExpiresAtAsc(
                now, now.plusDays(EXPIRY_NOTICE_DAYS))) {
            expiry.setNotified(true);
            expiryScheduleRepository.save(expiry);
            notify(expiry.getCustomer(), "Points Expiring Soon",
                    expiry.getPointsRemaining() + " points expire on " + expiry.getExpiresAt().toLocalDate() + ".", null);
        }
    }

    @Transactional
    public LoyaltyWalletDTO manualAdjust(ManualAdjustmentRequestDTO request, User admin) {
        requireAdmin(admin);
        User customer = userRepository.findById(request.getCustomerId()).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        LoyaltyWallet wallet = walletRepository.findByCustomerIdForUpdate(customer.getId()).orElseGet(() -> createWallet(customer));
        long points = request.getPoints();
        if (points < 0 && wallet.getAvailablePoints() + points < 0) {
            throw new BusinessValidationException("Adjustment would make wallet negative");
        }
        wallet.setAvailablePoints(wallet.getAvailablePoints() + points);
        if (points > 0) {
            wallet.setLifetimePoints(wallet.getLifetimePoints() + points);
            wallet.setTotalPointsEarned(wallet.getTotalPointsEarned() + points);
        }
        wallet.setTier(resolveTier(wallet.getLifetimePoints()));
        walletRepository.save(wallet);
        saveTransaction(customer, null, LoyaltyTransactionType.MANUAL_ADJUSTMENT, points, null,
                "Admin adjustment: " + request.getReason(), LoyaltyTransactionStatus.AVAILABLE,
                "manual-adjust-" + customer.getId() + "-" + UUID.randomUUID(), "admin=" + admin.getId());
        return toWalletDTO(wallet, PageRequestLite.firstFive());
    }

    @Transactional
    public LoyaltyWalletDTO freezeWallet(Long customerId, boolean frozen, String reason, User admin) {
        requireAdmin(admin);
        LoyaltyWallet wallet = walletRepository.findByCustomerIdForUpdate(customerId).orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        wallet.setFrozen(frozen);
        wallet.setSuspicious(frozen);
        wallet.setFraudReason(reason);
        walletRepository.save(wallet);
        return toWalletDTO(wallet, PageRequestLite.firstFive());
    }

    @Transactional(readOnly = true)
    public LoyaltyAnalyticsDTO analytics(int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(Math.max(1, days));
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Object[] row : transactionRepository.dailyTrend(start)) {
            trend.add(Map.of("date", row[0], "type", row[1], "points", row[2]));
        }
        return LoyaltyAnalyticsDTO.builder()
                .totalWallets(walletRepository.count())
                .frozenWallets(walletRepository.countByFrozenTrue())
                .suspiciousWallets(walletRepository.countBySuspiciousTrue())
                .pointsEarned(transactionRepository.sumPointsSince(LoyaltyTransactionType.EARN, start))
                .pointsRedeemed(transactionRepository.sumPointsSince(LoyaltyTransactionType.REDEEM, start))
                .pointsExpired(transactionRepository.sumPointsSince(LoyaltyTransactionType.EXPIRE, start))
                .dailyTrend(trend)
                .build();
    }

    @Transactional
    public RewardRule saveRule(RewardRuleRequestDTO dto) {
        return rewardRuleRepository.save(RewardRule.builder()
                .name(dto.getName())
                .ruleType(dto.getRuleType())
                .rewardRate(dto.getRewardRate())
                .multiplier(dto.getMultiplier())
                .category(dto.getCategory())
                .sellerId(dto.getSellerId())
                .active(dto.isActive())
                .priority(dto.getPriority() != null ? dto.getPriority() : 100)
                .startsAt(dto.getStartsAt())
                .endsAt(dto.getEndsAt())
                .build());
    }

    @Transactional
    public TierConfig saveTier(TierConfigRequestDTO dto) {
        TierConfig tier = tierConfigRepository.findByTier(dto.getTier()).orElseGet(TierConfig::new);
        tier.setTier(dto.getTier());
        tier.setMinLifetimePoints(dto.getMinLifetimePoints());
        tier.setRewardMultiplier(dto.getRewardMultiplier());
        tier.setBenefits(dto.getBenefits());
        tier.setActive(dto.isActive());
        return tierConfigRepository.save(tier);
    }

    public int expiryNoticeDays() {
        return EXPIRY_NOTICE_DAYS;
    }

    private void restoreRedemptions(Order order, LoyaltyWallet wallet) {
        for (RedemptionHistory redemption : redemptionHistoryRepository.findByOrderIdAndRestoredFalse(order.getId())) {
            wallet.setAvailablePoints(wallet.getAvailablePoints() + redemption.getPointsRedeemed());
            wallet.setRedeemedPoints(Math.max(0L, wallet.getRedeemedPoints() - redemption.getPointsRedeemed()));
            redemption.setRestored(true);
            redemption.setRestoredAt(LocalDateTime.now());
            redemptionHistoryRepository.save(redemption);
            saveTransaction(order.getUser(), order, LoyaltyTransactionType.REDEMPTION_RESTORE, redemption.getPointsRedeemed(),
                    redemption.getDiscountAmount(), "Restored redeemed points for order #" + order.getId(),
                    LoyaltyTransactionStatus.AVAILABLE, "restore-redemption-" + redemption.getId(), null);
        }
    }

    private RedemptionQuoteDTO quote(LoyaltyWallet wallet, BigDecimal orderTotal, Long requestedPoints) {
        BigDecimal total = orderTotal != null ? orderTotal : BigDecimal.ZERO;
        Long requested = requestedPoints != null ? requestedPoints : 0L;
        if (requested <= 0 || total.compareTo(MIN_REDEMPTION_ORDER_AMOUNT) < 0 || wallet.isFrozen()) {
            return RedemptionQuoteDTO.builder()
                    .requestedPoints(requested)
                    .approvedPoints(0L)
                    .discountAmount(BigDecimal.ZERO)
                    .conversionRate(POINT_TO_RUPEE)
                    .maxRedemptionAmount(total.multiply(MAX_REDEMPTION_PERCENT).setScale(2, RoundingMode.HALF_UP))
                    .minimumOrderAmount(MIN_REDEMPTION_ORDER_AMOUNT)
                    .message(wallet.isFrozen() ? "Wallet is frozen" : "Order does not qualify for redemption")
                    .build();
        }
        BigDecimal maxAmount = total.multiply(MAX_REDEMPTION_PERCENT).setScale(2, RoundingMode.HALF_UP);
        Long maxByOrder = maxAmount.divide(POINT_TO_RUPEE, 0, RoundingMode.DOWN).longValue();
        Long approved = Math.max(0L, Math.min(requested, Math.min(wallet.getAvailablePoints(), maxByOrder)));
        return RedemptionQuoteDTO.builder()
                .requestedPoints(requested)
                .approvedPoints(approved)
                .discountAmount(pointsToValue(approved))
                .conversionRate(POINT_TO_RUPEE)
                .maxRedemptionAmount(maxAmount)
                .minimumOrderAmount(MIN_REDEMPTION_ORDER_AMOUNT)
                .message(approved > 0 ? "Redemption applied" : "No points available")
                .build();
    }

    private void validateRedeem(LoyaltyWallet wallet, Long points) {
        if (points == null || points <= 0) {
            throw new BusinessValidationException("Points must be greater than zero");
        }
        if (wallet.getAvailablePoints() < points) {
            throw new BusinessValidationException("Insufficient loyalty points");
        }
    }

    private BigDecimal pointsToValue(Long points) {
        return POINT_TO_RUPEE.multiply(BigDecimal.valueOf(points != null ? points : 0L)).setScale(2, RoundingMode.HALF_UP);
    }

    private LoyaltyWallet ensureWallet(User user) {
        return walletRepository.findByCustomer(user).orElseGet(() -> createWallet(user));
    }

    private LoyaltyWallet getWalletEntity(Long userId) {
        return walletRepository.findByCustomerId(userId).orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private LoyaltyWallet createWallet(User user) {
        return walletRepository.save(LoyaltyWallet.builder().customer(user).tier(resolveTier(0L)).build());
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private LoyaltyTransaction saveTransaction(User user, Order order, LoyaltyTransactionType type, Long points,
                                               BigDecimal value, String description, LoyaltyTransactionStatus status,
                                               String referenceKey, String metadata) {
        return transactionRepository.findByReferenceKey(referenceKey).orElseGet(() -> transactionRepository.save(LoyaltyTransaction.builder()
                .customer(user)
                .order(order)
                .transactionType(type)
                .points(points)
                .monetaryValue(value)
                .description(description)
                .status(status)
                .referenceKey(referenceKey)
                .metadata(metadata)
                .createdAt(LocalDateTime.now())
                .availableAt(status == LoyaltyTransactionStatus.AVAILABLE ? LocalDateTime.now() : null)
                .build()));
    }

    private LoyaltyTier resolveTier(Long lifetimePoints) {
        ensureDefaultConfiguration();
        return tierConfigRepository.findByActiveTrueOrderByMinLifetimePointsAsc().stream()
                .filter(t -> lifetimePoints >= t.getMinLifetimePoints())
                .map(TierConfig::getTier)
                .reduce((first, second) -> second)
                .orElse(LoyaltyTier.BRONZE);
    }

    private LoyaltyTier nextTier(LoyaltyTier tier) {
        return switch (tier) {
            case BRONZE -> LoyaltyTier.SILVER;
            case SILVER -> LoyaltyTier.GOLD;
            case GOLD -> LoyaltyTier.PLATINUM;
            default -> null;
        };
    }

    private Long threshold(LoyaltyTier tier) {
        if (tier == null) {
            return 0L;
        }
        return tierConfigRepository.findByTier(tier).map(TierConfig::getMinLifetimePoints).orElse(0L);
    }

    private String tierBenefits(LoyaltyTier tier) {
        return tierConfigRepository.findByTier(tier).map(TierConfig::getBenefits).orElse("Earn rewards on eligible orders.");
    }

    private LoyaltyWalletDTO toWalletDTO(LoyaltyWallet wallet, Pageable pageable) {
        LoyaltyTier next = nextTier(wallet.getTier());
        Long nextThreshold = next != null ? threshold(next) : threshold(wallet.getTier());
        Long currentThreshold = threshold(wallet.getTier());
        Long span = Math.max(1L, nextThreshold - currentThreshold);
        int progress = next == null ? 100 : (int) Math.min(100L, Math.max(0L,
                ((wallet.getLifetimePoints() - currentThreshold) * 100) / span));
        return LoyaltyWalletDTO.builder()
                .id(wallet.getId())
                .customerId(wallet.getCustomer().getId())
                .totalPointsEarned(wallet.getTotalPointsEarned())
                .availablePoints(wallet.getAvailablePoints())
                .redeemedPoints(wallet.getRedeemedPoints())
                .expiredPoints(wallet.getExpiredPoints())
                .lifetimePoints(wallet.getLifetimePoints())
                .pendingPoints(wallet.getPendingPoints())
                .tier(wallet.getTier())
                .nextTier(next)
                .pointsToNextTier(next == null ? 0L : Math.max(0L, nextThreshold - wallet.getLifetimePoints()))
                .tierProgressPercent(progress)
                .frozen(wallet.isFrozen())
                .suspicious(wallet.isSuspicious())
                .benefits(tierBenefits(wallet.getTier()))
                .lastEarnedAt(wallet.getLastEarnedAt())
                .lastRedeemedAt(wallet.getLastRedeemedAt())
                .recentTransactions(transactionRepository.findByCustomerIdOrderByCreatedAtDesc(wallet.getCustomer().getId(), pageable)
                        .map(this::toTransactionDTO).getContent())
                .expiringPoints(expiryScheduleRepository.findByCustomerIdAndExpiredFalseOrderByExpiresAtAsc(wallet.getCustomer().getId())
                        .stream().limit(10).map(e -> ExpiryScheduleDTO.builder()
                                .id(e.getId()).pointsRemaining(e.getPointsRemaining()).expiresAt(e.getExpiresAt()).notified(e.isNotified()).build())
                        .toList())
                .build();
    }

    private LoyaltyTransactionDTO toTransactionDTO(LoyaltyTransaction tx) {
        return LoyaltyTransactionDTO.builder()
                .id(tx.getId())
                .customerId(tx.getCustomer().getId())
                .orderId(tx.getOrder() != null ? tx.getOrder().getId() : null)
                .transactionType(tx.getTransactionType())
                .points(tx.getPoints())
                .monetaryValue(tx.getMonetaryValue())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .status(tx.getStatus())
                .build();
    }

    private void notify(User user, String title, String message, Long entityId) {
        try {
            notificationService.createNotification(user, title, message, NotificationType.LOYALTY_UPDATE, entityId);
        } catch (Exception e) {
            log.warn("Failed to create loyalty notification for user {}: {}", user != null ? user.getId() : null, e.getMessage());
        }
    }

    private void requireAdmin(User user) {
        if (user == null || user.getRole() != Role.ADMIN) {
            throw new BusinessValidationException("Admin access required");
        }
    }

    private void ensureDefaultConfiguration() {
        if (tierConfigRepository.count() == 0) {
            tierConfigRepository.saveAll(List.of(
                    TierConfig.builder().tier(LoyaltyTier.BRONZE).minLifetimePoints(0L).rewardMultiplier(new BigDecimal("1.00")).benefits("Base rewards and member offers.").build(),
                    TierConfig.builder().tier(LoyaltyTier.SILVER).minLifetimePoints(500L).rewardMultiplier(new BigDecimal("1.10")).benefits("10% bonus rewards and early promotions.").build(),
                    TierConfig.builder().tier(LoyaltyTier.GOLD).minLifetimePoints(2000L).rewardMultiplier(new BigDecimal("1.25")).benefits("25% bonus rewards, priority support, and premium campaigns.").build(),
                    TierConfig.builder().tier(LoyaltyTier.PLATINUM).minLifetimePoints(5000L).rewardMultiplier(new BigDecimal("1.50")).benefits("50% bonus rewards, VIP support, and exclusive launches.").build()
            ));
        }
        if (rewardRuleRepository.count() == 0) {
            rewardRuleRepository.saveAll(List.of(
                    RewardRule.builder().name("Base reward").ruleType(RewardRuleType.BASE).rewardRate(new BigDecimal("0.01")).priority(100).build(),
                    RewardRule.builder().name("Electronics reward").ruleType(RewardRuleType.CATEGORY).category("Electronics").rewardRate(new BigDecimal("0.01")).priority(20).build(),
                    RewardRule.builder().name("Fashion reward").ruleType(RewardRuleType.CATEGORY).category("Fashion").rewardRate(new BigDecimal("0.05")).priority(20).build()
            ));
        }
    }

    private static class PageRequestLite {
        static Pageable firstFive() {
            return org.springframework.data.domain.PageRequest.of(0, 5);
        }
    }
}
