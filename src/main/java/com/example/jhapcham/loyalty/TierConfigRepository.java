package com.example.jhapcham.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TierConfigRepository extends JpaRepository<TierConfig, Long> {
    Optional<TierConfig> findByTier(LoyaltyTier tier);
    List<TierConfig> findByActiveTrueOrderByMinLifetimePointsAsc();
}
