package com.example.jhapcham.loyalty;

import com.example.jhapcham.user.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LoyaltyWalletRepository extends JpaRepository<LoyaltyWallet, Long> {
    Optional<LoyaltyWallet> findByCustomer(User customer);
    Optional<LoyaltyWallet> findByCustomerId(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM LoyaltyWallet w WHERE w.customer.id = :customerId")
    Optional<LoyaltyWallet> findByCustomerIdForUpdate(@Param("customerId") Long customerId);

    long countByFrozenTrue();
    long countBySuspiciousTrue();
}
