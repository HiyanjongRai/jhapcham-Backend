package com.example.jhapcham.campaign;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignScheduler {

    private final CampaignRepository campaignRepository;
    private final CampaignProductRepository campaignProductRepository;
    private final ProductRepository productRepository;
    private final com.example.jhapcham.notification.NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void runCampaignEngine() {
        LocalDateTime now = LocalDateTime.now();

        List<Campaign> campaignsToActivate = campaignRepository.findByStatusAndStartTimeBefore(CampaignStatus.UPCOMING,
                now);
        for (Campaign campaign : campaignsToActivate) {
            activateCampaign(campaign);
        }

        List<Campaign> campaignsToExpire = campaignRepository.findByStatusAndEndTimeBefore(CampaignStatus.ACTIVE, now);
        for (Campaign campaign : campaignsToExpire) {
            expireCampaign(campaign);
        }
    }

    private void activateCampaign(Campaign campaign) {
        log.info("Activating campaign: {}", campaign.getName());
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaignRepository.save(campaign);

        List<CampaignProduct> campaignProducts = campaignProductRepository.findByCampaignIdAndStatus(campaign.getId(),
                CampaignProductStatus.APPROVED);

        for (CampaignProduct cp : campaignProducts) {
            applyBestCampaignPrice(cp.getProduct());
        }

        // Notify customers that sale has started
        notificationService.notifyAllCustomers(
                "Sale is LIVE: " + campaign.getName(),
                "Grab your favorite products at huge discounts! Shop now before the sale ends.",
                campaign.getId());
    }

    private void expireCampaign(Campaign campaign) {
        log.info("Expiring campaign: {}", campaign.getName());
        campaign.setStatus(CampaignStatus.EXPIRED);
        campaignRepository.save(campaign);

        List<CampaignProduct> campaignProducts = campaignProductRepository.findByCampaignId(campaign.getId());
        for (CampaignProduct cp : campaignProducts) {
            applyBestCampaignPrice(cp.getProduct());
        }
    }

    private void applyBestCampaignPrice(Product product) {
        List<CampaignProduct> activeCampaignProducts = campaignProductRepository
                .findActiveCampaignsForProduct(product.getId());

        if (activeCampaignProducts.isEmpty()) {
            // Revert to original sale if exists
            java.math.BigDecimal price = product.getPrice();

            if (product.getSalePercentage() != null
                    && product.getSalePercentage().compareTo(java.math.BigDecimal.ZERO) > 0) {
                // CASE 1: Restore Percentage Sale
                // Recalculate based on stored percentage
                java.math.BigDecimal pct = product.getSalePercentage();
                java.math.BigDecimal discount = price
                        .multiply(pct)
                        .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

                java.math.BigDecimal salePrice = price
                        .subtract(discount)
                        .setScale(2, java.math.RoundingMode.HALF_UP);

                product.setSalePrice(salePrice);
                product.setDiscountPrice(discount);
                product.setOnSale(true);
                product.setSaleLabel(null); // Revert to standard % label
                product.setSaleEndTime(null);

            } else if (product.getDiscountPrice() != null
                    && product.getDiscountPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                // CASE 2: Restore Fixed Price Sale
                // Stored 'discountPrice' is the saved amount (Price - SalePrice)
                // So SalePrice = Price - discountPrice
                java.math.BigDecimal discount = product.getDiscountPrice();
                java.math.BigDecimal salePrice = price.subtract(discount);

                if (salePrice.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    product.setSalePrice(salePrice);
                    product.setOnSale(true);
                    product.setSaleLabel(null);
                    product.setSaleEndTime(null);
                } else {
                    // Invalid state, reset
                    product.setOnSale(false);
                    product.setSalePrice(null);
                    product.setSaleLabel(null);
                    product.setSaleEndTime(null);
                }
            } else {
                // No previous sale, turn off
                product.setOnSale(false);
                product.setSalePrice(null);
                product.setSaleLabel(null);
                product.setSaleEndTime(null);
            }
        } else {
            activeCampaignProducts.sort((cp1, cp2) -> {
                int p1 = cp1.getCampaign().getPriority();
                int p2 = cp2.getCampaign().getPriority();
                if (p1 != p2)
                    return p2 - p1;
                return cp1.getSalePrice().compareTo(cp2.getSalePrice());
            });

            CampaignProduct best = activeCampaignProducts.get(0);
            product.setOnSale(true);
            product.setSalePrice(best.getSalePrice());
            product.setSaleLabel(best.getCampaign().getName());
            product.setSaleEndTime(best.getCampaign().getEndTime());
        }
        productRepository.save(product);
    }
}
