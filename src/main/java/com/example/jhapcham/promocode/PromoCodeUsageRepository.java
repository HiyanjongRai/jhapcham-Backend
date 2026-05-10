package com.example.jhapcham.promocode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, Long> {

    Optional<PromoCodeUsage> findByPromoCodeAndUser_Id(PromoCode promoCode, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM PromoCodeUsage u WHERE u.promoCode = :promoCode AND u.user.id = :userId")
    Optional<PromoCodeUsage> findByPromoCodeAndUserIdForUpdate(@Param("promoCode") PromoCode promoCode,
            @Param("userId") Long userId);
}
