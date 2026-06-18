package com.example.jhapcham.loyalty.persistence;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TierConfigRepository extends JpaRepository<TierConfig, Long> {
    Optional<TierConfig> findByTier(LoyaltyTier tier);
    List<TierConfig> findByActiveTrueOrderByMinLifetimePointsAsc();
}
