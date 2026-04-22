package com.example.jhapcham.promocode;

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
}
