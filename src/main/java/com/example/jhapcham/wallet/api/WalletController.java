package com.example.jhapcham.wallet.api;

import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.security.CurrentUserService;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.user.domain.User;
import com.example.jhapcham.wallet.application.WalletService;
import com.example.jhapcham.wallet.domain.WalletTransaction.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Customer and admin APIs for the wallet system.
 *
 * <p>Customer endpoints:
 * <ul>
 *   <li>GET /api/v1/wallet/balance — current balance</li>
 *   <li>GET /api/v1/wallet/transactions — paginated history</li>
 * </ul>
 *
 * <p>Admin endpoints:
 * <ul>
 *   <li>POST /api/v1/wallet/admin/adjust — manual adjustment</li>
 *   <li>GET  /api/v1/wallet/admin/balance/{userId} — check any user's balance</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final CurrentUserService currentUserService;

    /** Returns the authenticated user's wallet balance. */
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            return ResponseEntity.ok(Map.of(
                "userId",   user.getId(),
                "balance",  walletService.getBalance(user.getId()),
                "currency", "NPR"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /** Returns paginated wallet transaction history. */
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            return ResponseEntity.ok(walletService.getTransactionHistory(
                    user.getId(), PageRequest.of(page, size)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /** Admin: check any user's wallet balance. */
    @GetMapping("/admin/balance/{userId}")
    public ResponseEntity<?> getBalanceForUser(
            @PathVariable Long userId,
            Authentication authentication) {
        try {
            User admin = currentUserService.requireUser(authentication);
            currentUserService.requireAdmin(admin);
            return ResponseEntity.ok(Map.of(
                "userId",   userId,
                "balance",  walletService.getBalance(userId),
                "currency", "NPR"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Admin: manually credit or debit a user's wallet.
     * Body: { "userId": 123, "amount": 500.00, "type": "CREDIT", "description": "Goodwill refund" }
     */
    @PostMapping("/admin/adjust")
    public ResponseEntity<?> adminAdjust(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        try {
            User admin = currentUserService.requireUser(authentication);
            currentUserService.requireAdmin(admin);

            Long userId      = Long.parseLong(body.get("userId").toString());
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            TransactionType type = TransactionType.valueOf(body.get("type").toString().toUpperCase());
            String description = body.getOrDefault("description", "Admin adjustment").toString();

            return ResponseEntity.ok(walletService.adminAdjust(admin, userId, amount, type, description));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
