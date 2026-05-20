package com.example.jhapcham.loyalty;

import com.example.jhapcham.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {
    private final LoyaltyService loyaltyService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;
    private final RewardRuleRepository rewardRuleRepository;
    private final TierConfigRepository tierConfigRepository;
    private final LoyaltyWalletRepository walletRepository;

    @GetMapping("/wallet")
    public LoyaltyWalletDTO wallet(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        return loyaltyService.getWallet(user.getId());
    }

    @GetMapping("/my-points")
    public LoyaltyPointsDTO myPoints(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        return loyaltyService.getUserLoyaltyPoints(user.getId());
    }

    @GetMapping("/transactions")
    public Page<LoyaltyTransactionDTO> transactions(Authentication authentication, Pageable pageable) {
        User user = currentUserService.requireUser(authentication);
        return loyaltyService.getTransactions(user.getId(), pageable);
    }

    @PostMapping("/redemption/quote")
    public RedemptionQuoteDTO redemptionQuote(Authentication authentication, @RequestBody Map<String, Object> body) {
        User user = currentUserService.requireUser(authentication);
        BigDecimal orderTotal = new BigDecimal(String.valueOf(body.getOrDefault("orderTotal", "0")));
        Long points = Long.valueOf(String.valueOf(body.getOrDefault("points", "0")));
        return loyaltyService.quoteRedemption(user.getId(), orderTotal, points);
    }

    @PostMapping("/redeem")
    public LoyaltyPointsDTO redeem(@RequestBody Map<String, Long> body, Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        return loyaltyService.redeemPoints(user.getId(), body.get("points"));
    }

    @GetMapping("/admin/wallets")
    public Page<LoyaltyWallet> wallets(Authentication authentication, Pageable pageable) {
        User admin = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(admin);
        return walletRepository.findAll(pageable);
    }

    @GetMapping("/admin/analytics")
    public LoyaltyAnalyticsDTO analytics(Authentication authentication, @RequestParam(defaultValue = "30") int days) {
        User admin = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(admin);
        return loyaltyService.analytics(days);
    }

    @PostMapping("/admin/rules")
    public RewardRule createRule(Authentication authentication, @Valid @RequestBody RewardRuleRequestDTO dto) {
        User admin = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(admin);
        return loyaltyService.saveRule(dto);
    }

    @GetMapping("/admin/rules")
    public Page<RewardRule> rules(Authentication authentication, @RequestParam(required = false) Boolean active, Pageable pageable) {
        User admin = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(admin);
        return active == null ? rewardRuleRepository.findAll(pageable) : rewardRuleRepository.findByActive(active, pageable);
    }

    @PostMapping("/admin/tiers")
    public TierConfig saveTier(Authentication authentication, @Valid @RequestBody TierConfigRequestDTO dto) {
        User admin = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(admin);
        return loyaltyService.saveTier(dto);
    }

    @GetMapping("/admin/tiers")
    public java.util.List<TierConfig> tiers(Authentication authentication) {
        User admin = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(admin);
        return tierConfigRepository.findAll();
    }

    @PostMapping("/admin/adjust")
    public LoyaltyWalletDTO adjust(Authentication authentication, @Valid @RequestBody ManualAdjustmentRequestDTO dto) {
        User admin = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(admin);
        return loyaltyService.manualAdjust(dto, admin);
    }

    @PostMapping("/admin/wallets/{customerId}/freeze")
    public LoyaltyWalletDTO freeze(Authentication authentication, @PathVariable Long customerId, @RequestBody Map<String, Object> body) {
        User admin = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(admin);
        boolean frozen = Boolean.parseBoolean(String.valueOf(body.getOrDefault("frozen", "true")));
        String reason = String.valueOf(body.getOrDefault("reason", "Manual admin review"));
        return loyaltyService.freezeWallet(customerId, frozen, reason, admin);
    }

    @PostMapping("/admin/expiry/run")
    public ResponseEntity<Map<String, String>> runExpiry(Authentication authentication) {
        User admin = currentUserService.requireUser(authentication);
        currentUserService.requireAdmin(admin);
        loyaltyService.processExpiry();
        return ResponseEntity.ok(Map.of("status", "completed"));
    }
}
