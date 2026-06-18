package com.example.jhapcham.wallet.persistence;

import com.example.jhapcham.wallet.domain.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);
    Page<WalletTransaction> findByWalletUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
