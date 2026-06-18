package com.example.jhapcham.campaign.persistence;


import com.example.jhapcham.campaign.application.*;
import com.example.jhapcham.campaign.domain.*;
import com.example.jhapcham.campaign.dto.*;
import com.example.jhapcham.campaign.persistence.*;
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
