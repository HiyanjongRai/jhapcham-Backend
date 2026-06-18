package com.example.jhapcham.promocode.persistence;


import com.example.jhapcham.promocode.application.*;
import com.example.jhapcham.promocode.domain.*;
import com.example.jhapcham.promocode.dto.*;
import com.example.jhapcham.promocode.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

    Optional<PromoCode> findByCode(String code);

    List<PromoCode> findBySellerId(Long sellerId);

    boolean existsByCode(String code);

    /**
     * Find promo code with pessimistic lock to prevent race conditions during usage increment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PromoCode p WHERE p.code = :code")
    Optional<PromoCode> findByCodeForUpdate(@Param("code") String code);

    /**
     * Atomically increment usage count using database-level update.
     * This is safer than read-modify-write and prevents race conditions.
     */
    @Modifying
    @Query("UPDATE PromoCode p SET p.usedCount = p.usedCount + 1 WHERE p.code = :code")
    int incrementUsageCount(@Param("code") String code);

    @Query("""
            SELECT p FROM PromoCode p
            WHERE p.isActive = true
              AND p.usedCount < p.usageLimit
              AND p.startDate <= :now
              AND p.endDate >= :now
            ORDER BY p.endDate ASC, p.id DESC
            """)
    List<PromoCode> findActivePromoCodes(@Param("now") LocalDateTime now);

    @Query("""
            SELECT p FROM PromoCode p
            WHERE p.id = :id
              AND p.isActive = true
              AND p.usedCount < p.usageLimit
              AND p.startDate <= :now
              AND p.endDate >= :now
            """)
    Optional<PromoCode> findActivePromoCodeById(@Param("id") Long id, @Param("now") LocalDateTime now);
}
