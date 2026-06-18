package com.example.jhapcham.product.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HomepageResponseDto {
    private List<HomepageBannerDto> banners;
    private List<ProductCardDto> topSellingProducts;
    private List<ProductCardDto> mostLovedProducts;
    private List<ProductCardDto> discountProducts;
    private List<ProductCardDto> saleProducts;
    private List<ProductCardDto> newArrivals;
    private List<ProductCardDto> featuredProducts;
    private List<ProductCardDto> trendingProducts;
    private List<HomepagePromoCodeDto> activePromoCodes;
    private List<HomepageCampaignDto> activeCampaigns;
}
