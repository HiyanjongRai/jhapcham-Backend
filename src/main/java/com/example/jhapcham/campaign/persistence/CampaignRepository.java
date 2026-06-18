package com.example.jhapcham.campaign.persistence;


import com.example.jhapcham.campaign.application.*;
import com.example.jhapcham.campaign.domain.*;
import com.example.jhapcham.campaign.dto.*;
import com.example.jhapcham.campaign.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByStatus(CampaignStatus status);

    List<Campaign> findByStatusIn(List<CampaignStatus> statuses);

    List<Campaign> findByStatusAndStartTimeBefore(CampaignStatus status, LocalDateTime time);

    List<Campaign> findByStatusAndEndTimeBefore(CampaignStatus status, LocalDateTime time);

    // Find colliding active campaigns (for conflict resolution logic if needed
    // later)
    @Query("SELECT c FROM Campaign c WHERE c.status = 'ACTIVE'")
    List<Campaign> findAllActive();

    @Query("""
            SELECT c FROM Campaign c
            WHERE c.status = 'ACTIVE'
              AND c.startTime <= :now
              AND c.endTime >= :now
            ORDER BY c.priority DESC, c.endTime ASC, c.id DESC
            """)
    List<Campaign> findCurrentlyActive(@Param("now") LocalDateTime now);

    @Query("""
            SELECT c FROM Campaign c
            WHERE c.id = :id
              AND c.status = 'ACTIVE'
              AND c.startTime <= :now
              AND c.endTime >= :now
            """)
    java.util.Optional<Campaign> findCurrentlyActiveById(@Param("id") Long id, @Param("now") LocalDateTime now);
}
