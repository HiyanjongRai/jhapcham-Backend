package com.example.jhapcham.loyalty.persistence;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RewardRuleRepository extends JpaRepository<RewardRule, Long> {
    Page<RewardRule> findByActive(boolean active, Pageable pageable);

    @Query("""
            SELECT r FROM RewardRule r
            WHERE r.active = true
            AND (r.startsAt IS NULL OR r.startsAt <= :now)
            AND (r.endsAt IS NULL OR r.endsAt >= :now)
            ORDER BY r.priority ASC
            """)
    List<RewardRule> findActiveRules(@Param("now") LocalDateTime now);
}
