package com.example.jhapcham.product.application;

import com.example.jhapcham.banner.application.BannerService;
import com.example.jhapcham.campaign.application.CampaignService;
import com.example.jhapcham.product.dto.HomepageBannerDto;
import com.example.jhapcham.product.dto.HomepageCampaignDto;
import com.example.jhapcham.product.dto.HomepagePromoCodeDto;
import com.example.jhapcham.product.dto.HomepageResponseDto;
import com.example.jhapcham.product.dto.ProductCardDto;
import com.example.jhapcham.product.persistence.ProductRepository;
import com.example.jhapcham.promocode.application.PromoCodeService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomepageAggregationService {

    private static final int DEFAULT_SECTION_LIMIT = 12;
    private static final BigDecimal SALE_THRESHOLD = BigDecimal.valueOf(0.10);

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final BannerService bannerService;
    private final PromoCodeService promoCodeService;
    private final CampaignService campaignService;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = com.example.jhapcham.config.CacheConfig.HOMEPAGE, key = "'aggregate:' + #limit")
    public HomepageResponseDto getHomepage(int limit) {
        int safeLimit = normalizeLimit(limit);
        return HomepageResponseDto.builder()
                .banners(getBanners())
                .topSellingProducts(getTopSellingProducts(safeLimit))
                .mostLovedProducts(getMostLovedProducts(safeLimit))
                .discountProducts(getHighestDiscountProducts(safeLimit))
                .saleProducts(getSaleProducts(safeLimit))
                .newArrivals(getNewArrivalProducts(safeLimit))
                .featuredProducts(getFeaturedProducts(safeLimit))
                .trendingProducts(getTrendingProducts(safeLimit))
                .activePromoCodes(getActivePromoCodes())
                .activeCampaigns(getActiveCampaigns())
                .build();
    }

    @Transactional(readOnly = true)
    public List<HomepageBannerDto> getBanners() {
        return bannerService.getHomepageBanners();
    }

    @Transactional(readOnly = true)
    public List<ProductCardDto> getTopSellingProducts(int limit) {
        return productRepository.findTopSellingProductCards(pageable(limit)).stream()
                .map(productService::toProductCardDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductCardDto> getMostLovedProducts(int limit) {
        return productRepository.findMostLovedProductCards(pageable(limit)).stream()
                .map(productService::toProductCardDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductCardDto> getHighestDiscountProducts(int limit) {
        return productRepository.findHighestDiscountProductCards(pageable(limit)).stream()
                .map(productService::toProductCardDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductCardDto> getSaleProducts(int limit) {
        return productRepository.findSaleProductCards(SALE_THRESHOLD, pageable(limit)).stream()
                .map(productService::toProductCardDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductCardDto> getNewArrivalProducts(int limit) {
        return productRepository.findNewArrivalProductCards(pageable(limit)).stream()
                .map(productService::toProductCardDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductCardDto> getFeaturedProducts(int limit) {
        return productRepository.findFeaturedProductCards(pageable(limit)).stream()
                .map(productService::toProductCardDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductCardDto> getTrendingProducts(int limit) {
        return productRepository.findTrendingProductCards(pageable(limit)).stream()
                .map(productService::toProductCardDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HomepagePromoCodeDto> getActivePromoCodes() {
        return promoCodeService.getActiveHomepagePromoCodes();
    }

    @Transactional(readOnly = true)
    public HomepagePromoCodeDto getActivePromoCode(Long id) {
        return promoCodeService.getActiveHomepagePromoCode(id);
    }

    @Transactional(readOnly = true)
    public List<HomepageCampaignDto> getActiveCampaigns() {
        return campaignService.getActiveHomepageCampaigns();
    }

    @Transactional(readOnly = true)
    public HomepageCampaignDto getActiveCampaign(Long id) {
        return campaignService.getActiveHomepageCampaign(id);
    }

    private Pageable pageable(int limit) {
        return PageRequest.of(0, normalizeLimit(limit));
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) return DEFAULT_SECTION_LIMIT;
        return Math.min(limit, 60);
    }
}
