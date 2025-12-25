package com.example.jhapcham.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignProductRepository extends JpaRepository<CampaignProduct, Long> {
    List<CampaignProduct> findByCampaignId(Long campaignId);

    List<CampaignProduct> findByCampaignIdAndStatus(Long campaignId, CampaignProductStatus status);

    boolean existsByCampaignIdAndProductId(Long campaignId, Long productId);

    @Query("SELECT cp FROM CampaignProduct cp JOIN cp.campaign c WHERE cp.product.id = :productId AND c.status = 'ACTIVE' AND cp.status = 'APPROVED'")
    List<CampaignProduct> findActiveCampaignsForProduct(Long productId);
}
