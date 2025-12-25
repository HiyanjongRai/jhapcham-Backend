package com.example.jhapcham.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
