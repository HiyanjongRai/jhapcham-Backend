package com.example.jhapcham.wallet.application;

import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.user.domain.User;
import com.example.jhapcham.wallet.domain.Wallet;
import com.example.jhapcham.wallet.domain.WalletTransaction;
import com.example.jhapcham.wallet.domain.WalletTransaction.TransactionType;
import com.example.jhapcham.wallet.persistence.WalletRepository;
import com.example.jhapcham.wallet.persistence.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Manages customer wallet balances and transaction history.
 *
 * <p>Wallets are auto-created on first credit. Supports:
 * <ul>
 *   <li>Instant wallet refund (STORE_CREDIT or WALLET_CREDIT refund type)</li>
 *   <li>Admin manual adjustments</li>
 *   <li>Complete transaction history</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    /**
     * Credits the customer's wallet — auto-creates wallet if it does not exist.
     * Used for instant store-credit refunds.
     *
     * @param userId      the customer to credit
     * @param amount      amount to credit (must be > 0)
     * @param refundId    optional refund ID for traceability
     * @param description human-readable description shown in transaction history
     * @return the wallet transaction record
     */
    @Transactional
    public WalletTransaction creditWallet(Long userId, BigDecimal amount, Long refundId, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Wallet credit amount must be greater than zero");
        }

        Wallet wallet = getOrCreateWallet(userId);
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .refundId(refundId)
                .type(TransactionType.CREDIT)
                .amount(amount)
                .balanceAfter(newBalance)
                .description(description)
                .createdBy("SYSTEM")
                .build();

        WalletTransaction saved = transactionRepository.save(txn);
        log.info("[Wallet] Credited Rs.{} to user #{} wallet. New balance: Rs.{} (refund #{})",
                amount, userId, newBalance, refundId);
        return saved;
    }

    /**
     * Debits the customer's wallet (for purchases using wallet balance).
     *
     * @throws BusinessValidationException if balance is insufficient
     */
    @Transactional
    public WalletTransaction debitWallet(Long userId, BigDecimal amount, String description, String initiatedBy) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Debit amount must be greater than zero");
        }

        Wallet wallet = requireWallet(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessValidationException(
                "Insufficient wallet balance. Available: Rs." + wallet.getBalance() + ", Required: Rs." + amount);
        }

        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.DEBIT)
                .amount(amount)
                .balanceAfter(newBalance)
                .description(description)
                .createdBy(initiatedBy)
                .build();

        WalletTransaction saved = transactionRepository.save(txn);
        log.info("[Wallet] Debited Rs.{} from user #{} wallet. New balance: Rs.{}", amount, userId, newBalance);
        return saved;
    }

    /**
     * Admin manual wallet adjustment (credit or debit).
     */
    @Transactional
    public WalletTransaction adminAdjust(User admin, Long userId, BigDecimal amount,
                                         TransactionType type, String description) {
        if (type == TransactionType.CREDIT) {
            return creditWallet(userId, amount, null, "[Admin Adjustment] " + description);
        } else {
            return debitWallet(userId, amount, "[Admin Adjustment] " + description, "ADMIN:" + admin.getId());
        }
    }

    /** Returns current wallet balance for a user. Returns 0 if wallet does not exist. */
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    /** Returns paginated transaction history for a user. */
    @Transactional(readOnly = true)
    public Page<WalletTransaction> getTransactionHistory(Long userId, Pageable pageable) {
        return transactionRepository.findByWalletUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Wallet does not exist — create one now
                    // We need a User reference: fetch from repository via a lazy proxy trick
                    // The save below uses userId through a proxy
                    log.info("[Wallet] Auto-creating wallet for user #{}", userId);
                    // Use a proxy to avoid loading the full User entity
                    Wallet newWallet = new Wallet();
                    newWallet.setCurrency("NPR");
                    newWallet.setBalance(BigDecimal.ZERO);
                    // Set user via raw query approach — requires a user object
                    // Injecting UserRepository would create a circular dep; use a simple proxy
                    com.example.jhapcham.user.domain.User userProxy = new com.example.jhapcham.user.domain.User();
                    userProxy.setId(userId);
                    newWallet.setUser(userProxy);
                    return walletRepository.save(newWallet);
                });
    }

    private Wallet requireWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user #" + userId));
    }
}
