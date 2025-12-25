package com.example.jhapcham.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SellerCampaignRepository extends JpaRepository<SellerCampaign, Long> {
    boolean existsByCampaignIdAndSellerId(Long campaignId, Long sellerId);

    List<SellerCampaign> findBySellerId(Long sellerId);

    List<SellerCampaign> findByCampaignId(Long campaignId);

    List<SellerCampaign> findByCampaign(Campaign campaign);
}
