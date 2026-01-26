package com.example.jhapcham.campaign;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductImage;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.seller.SellerProfile;
import com.example.jhapcham.seller.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignProductRepository campaignProductRepository;
    private final SellerCampaignRepository sellerCampaignRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final ProductRepository productRepository;
    private final com.example.jhapcham.common.FileStorageService fileStorageService;
    private final com.example.jhapcham.notification.NotificationService notificationService;

    public CampaignResponseDTO createCampaign(CampaignCreateRequestDTO dto) {
        String imagePath = null;
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            imagePath = fileStorageService.save(dto.getImage(), "campaign-images",
                    "campaign_" + System.currentTimeMillis());
        }

        Campaign campaign = Campaign.builder()
                .name(dto.getName())
                .type(dto.getType())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .discountType(dto.getDiscountType())
                .status(CampaignStatus.UPCOMING)
                .priority(dto.getPriority())
                .imagePath(imagePath)
                .build();
        Campaign savedCampaign = campaignRepository.save(campaign);

        // Notify sellers to join
        notificationService.notifyAllSellers(
                "New Campaign: " + savedCampaign.getName(),
                "A new " + savedCampaign.getType() + " campaign has been created. Join now to boost your sales!",
                savedCampaign.getId());

        return mapToCampaignDTO(savedCampaign);
    }

    public List<CampaignResponseDTO> getAllCampaigns() {
        return campaignRepository.findAll().stream().map(this::mapToCampaignDTO).toList();
    }

    public List<CampaignResponseDTO> getUpcomingCampaigns() {
        return campaignRepository.findByStatusIn(List.of(CampaignStatus.UPCOMING, CampaignStatus.ACTIVE))
                .stream().map(this::mapToCampaignDTO).toList();
    }

    @Transactional
    public void deleteCampaign(@org.springframework.lang.NonNull Long campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        // Keep track of products to recalculate prices after campaign is gone
        List<Product> productsToUpdate = (campaign.getCampaignProducts() != null)
                ? campaign.getCampaignProducts().stream().map(CampaignProduct::getProduct).distinct().toList()
                : List.of();

        // Delete the campaign; JPA CascadeType.ALL + orphanRemoval handles
        // CampaignProduct and SellerCampaign
        campaignRepository.delete(campaign);
        campaignRepository.flush(); // Ensure synchronization with DB immediately

        // Recalculate Prices for products that were in the campaign
        for (Product product : productsToUpdate) {
            recalculateProductPrice(product);
        }
    }

    @Transactional
    public void joinCampaign(@org.springframework.lang.NonNull Long sellerUserId, CampaignJoinRequestDTO dto) {
        // We don't need to instantiate User if we have findByUserId
        SellerProfile seller = sellerProfileRepository.findByUserId(sellerUserId)
                .orElseThrow(() -> new RuntimeException("Seller profile not found for user id: " + sellerUserId));

        Campaign campaign = campaignRepository.findById(dto.getCampaignId())
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (!sellerCampaignRepository.existsByCampaignIdAndSellerId(campaign.getId(), seller.getId())) {
            SellerCampaign sellerCampaign = SellerCampaign.builder()
                    .campaign(campaign)
                    .seller(seller)
                    .joinedAt(LocalDateTime.now())
                    .build();
            sellerCampaignRepository.save(sellerCampaign);
        }

        for (CampaignJoinRequestDTO.ProductJoinDTO productDto : dto.getProducts()) {
            Product product = productRepository.findById(productDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productDto.getProductId()));

            // Check if product belongs to this seller profile
            if (!product.getSellerProfile().getId().equals(seller.getId())) {
                throw new RuntimeException("Product does not belong to seller");
            }

            // Validate Stock for Campaign
            if (product.getStockQuantity() < 10) {
                throw new RuntimeException(
                        "Product " + product.getName() + " must have at least 10 stock to join a campaign.");
            }

            if (campaignProductRepository.existsByCampaignIdAndProductId(campaign.getId(), product.getId())) {
                continue;
            }

            CampaignProduct campaignProduct = CampaignProduct.builder()
                    .campaign(campaign)
                    .product(product)
                    .salePrice(productDto.getSalePrice())
                    .stockLimit(productDto.getStockLimit())
                    .status(CampaignProductStatus.PENDING)
                    .build();
            campaignProductRepository.save(campaignProduct);
        }
    }

    @Transactional
    public void approveProduct(@org.springframework.lang.NonNull Long campaignProductId) {
        CampaignProduct cp = campaignProductRepository.findById(campaignProductId)
                .orElseThrow(() -> new RuntimeException("Campaign Product not found"));
        cp.setStatus(CampaignProductStatus.APPROVED);
        campaignProductRepository.save(cp);

        // If campaign is already active, apply the price immediately
        if (cp.getCampaign().getStatus() == CampaignStatus.ACTIVE) {
            recalculateProductPrice(cp.getProduct());
        }
    }

    private void recalculateProductPrice(Product product) {
        // Logic similar to Scheduler's applyBestCampaignPrice
        List<CampaignProduct> activeCampaignProducts = campaignProductRepository
                .findActiveCampaignsForProduct(product.getId());

        if (activeCampaignProducts.isEmpty()) {
            // Revert to original sale if exists
            java.math.BigDecimal price = product.getPrice();

            if (product.getSalePercentage() != null
                    && product.getSalePercentage().compareTo(java.math.BigDecimal.ZERO) > 0) {
                // CASE 1: Restore Percentage Sale
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
                product.setSaleLabel(null);
                product.setSaleEndTime(null);

            } else if (product.getDiscountPrice() != null
                    && product.getDiscountPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                // CASE 2: Restore Fixed Price Sale
                java.math.BigDecimal discount = product.getDiscountPrice();
                java.math.BigDecimal salePrice = price.subtract(discount);

                if (salePrice.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    product.setSalePrice(salePrice);
                    product.setOnSale(true);
                    product.setSaleLabel(null);
                    product.setSaleEndTime(null);
                } else {
                    product.setOnSale(false);
                    product.setSalePrice(null);
                    product.setSaleLabel(null);
                    product.setSaleEndTime(null);
                }
            } else {
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

    @Transactional
    public void rejectProduct(@org.springframework.lang.NonNull Long campaignProductId) {
        CampaignProduct cp = campaignProductRepository.findById(campaignProductId)
                .orElseThrow(() -> new RuntimeException("Campaign Product not found"));
        cp.setStatus(CampaignProductStatus.REJECTED);
        campaignProductRepository.save(cp);
    }

    @Transactional(readOnly = true)
    public List<CampaignProductResponseDTO> getPendingProducts(@org.springframework.lang.NonNull Long campaignId) {
        return campaignProductRepository.findByCampaignIdAndStatus(campaignId, CampaignProductStatus.PENDING)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CampaignProductResponseDTO> getCampaignProducts(@org.springframework.lang.NonNull Long campaignId) {
        return campaignProductRepository.findByCampaignId(campaignId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CampaignProductResponseDTO> getPublicCampaignProducts(
            @org.springframework.lang.NonNull Long campaignId) {
        return campaignProductRepository.findByCampaignIdAndStatus(campaignId, CampaignProductStatus.APPROVED)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    private CampaignProductResponseDTO mapToDTO(CampaignProduct cp) {
        String img = null;
        if (cp.getProduct() != null && cp.getProduct().getImages() != null && !cp.getProduct().getImages().isEmpty()) {
            img = cp.getProduct().getImages().stream().findFirst().map(ProductImage::getImagePath).orElse(null);
        }

        String sellerName = "Unknown Seller";
        Long sellerId = null;
        if (cp.getProduct() != null && cp.getProduct().getSellerProfile() != null) {
            sellerName = cp.getProduct().getSellerProfile().getStoreName();
            sellerId = cp.getProduct().getSellerProfile().getId();
        }

        return CampaignProductResponseDTO.builder()
                .id(cp.getId())
                .productId(cp.getProduct() != null ? cp.getProduct().getId() : null)
                .productName(cp.getProduct() != null ? cp.getProduct().getName() : "Unknown Product")
                .productImage(img)
                .originalPrice(cp.getProduct() != null ? cp.getProduct().getPrice() : null)
                .salePrice(cp.getSalePrice())
                .stockLimit(cp.getStockLimit())
                .sellerName(sellerName)
                .storeName(sellerName)
                .sellerId(sellerId)
                .status(cp.getStatus())
                .build();
    }

    private CampaignResponseDTO mapToCampaignDTO(Campaign campaign) {
        return CampaignResponseDTO.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .type(campaign.getType())
                .startTime(campaign.getStartTime())
                .endTime(campaign.getEndTime())
                .discountType(campaign.getDiscountType())
                .status(campaign.getStatus())
                .priority(campaign.getPriority())
                .imagePath(campaign.getImagePath())
                .build();
    }
}
