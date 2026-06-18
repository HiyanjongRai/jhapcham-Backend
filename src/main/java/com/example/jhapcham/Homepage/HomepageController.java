package com.example.jhapcham.Homepage;

import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.product.application.BestSellerService;
import com.example.jhapcham.product.application.HomepageAggregationService;
import com.example.jhapcham.product.application.MostWishlistedService;
import com.example.jhapcham.product.application.ProductRecommendationService;
import com.example.jhapcham.product.application.TopRatedService;
import com.example.jhapcham.product.application.TrendingService;
import com.example.jhapcham.seller.application.TopSellerService;
import com.example.jhapcham.product.dto.BestSellerDTO;
import com.example.jhapcham.product.dto.MostWishlistedDTO;
import com.example.jhapcham.product.dto.RecommendationDTO;
import com.example.jhapcham.product.dto.TopRatedDTO;
import com.example.jhapcham.product.dto.TrendingDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomepageController {

    private static final int MAX_PAGE_SIZE = 60;

    private final BestSellerService bestSellerService;
    private final TopRatedService topRatedService;
    private final MostWishlistedService mostWishlistedService;
    private final TrendingService trendingService;
    private final ProductRecommendationService productRecommendationService;
    private final HomepageAggregationService homepageAggregationService;
    private final TopSellerService topSellerService;

    @GetMapping("/api/homepage")
    public ResponseEntity<?> getHomepage(@RequestParam(defaultValue = "12") int limit) {
        try {
            return ResponseEntity.ok(homepageAggregationService.getHomepage(Math.min(limit, 60)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch homepage: " + e.getMessage()));
        }
    }

    @GetMapping("/api/homepage/banners")
    public ResponseEntity<?> getHomepageBanners() {
        try {
            return ResponseEntity.ok(homepageAggregationService.getBanners());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch homepage banners: " + e.getMessage()));
        }
    }

    @GetMapping("/api/homepage/top-selling-products")
    public ResponseEntity<?> getHomepageTopSellingProducts(@RequestParam(defaultValue = "12") int limit) {
        try {
            return ResponseEntity.ok(homepageAggregationService.getTopSellingProducts(Math.min(limit, 60)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch top selling products: " + e.getMessage()));
        }
    }

    @GetMapping("/api/homepage/top-sellers")
    public ResponseEntity<?> getHomepageTopSellers(@RequestParam(defaultValue = "12") int limit) {
        try {
            return ResponseEntity.ok(topSellerService.getTopSellers(limit));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch top sellers: " + e.getMessage()));
        }
    }

    @GetMapping("/api/homepage/top-rated-sellers")
    public ResponseEntity<?> getHomepageTopRatedSellers(@RequestParam(defaultValue = "12") int limit) {
        try {
            return ResponseEntity.ok(topSellerService.getTopRatedSellers(limit));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch top rated sellers: " + e.getMessage()));
        }
    }

    @GetMapping("/api/homepage/most-loved-products")
    public ResponseEntity<?> getHomepageMostLovedProducts(@RequestParam(defaultValue = "12") int limit) {
        try {
            return ResponseEntity.ok(homepageAggregationService.getMostLovedProducts(Math.min(limit, 60)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch most loved products: " + e.getMessage()));
        }
    }

    @GetMapping("/api/homepage/highest-discount-products")
    public ResponseEntity<?> getHomepageHighestDiscountProducts(@RequestParam(defaultValue = "12") int limit) {
        try {
            return ResponseEntity.ok(homepageAggregationService.getHighestDiscountProducts(Math.min(limit, 60)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch discount products: " + e.getMessage()));
        }
    }

    @GetMapping("/api/homepage/sale-products")
    public ResponseEntity<?> getHomepageSaleProducts(@RequestParam(defaultValue = "12") int limit) {
        try {
            return ResponseEntity.ok(homepageAggregationService.getSaleProducts(Math.min(limit, 60)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch sale products: " + e.getMessage()));
        }
    }

    @GetMapping("/api/homepage/new-arrival-products")
    public ResponseEntity<?> getHomepageNewArrivalProducts(@RequestParam(defaultValue = "12") int limit) {
        try {
            return ResponseEntity.ok(homepageAggregationService.getNewArrivalProducts(Math.min(limit, 60)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch new arrivals: " + e.getMessage()));
        }
    }

    @GetMapping("/api/homepage/featured-products")
    public ResponseEntity<?> getHomepageFeaturedProducts(@RequestParam(defaultValue = "12") int limit) {
        try {
            return ResponseEntity.ok(homepageAggregationService.getFeaturedProducts(Math.min(limit, 60)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch featured products: " + e.getMessage()));
        }
    }

    @GetMapping("/api/homepage/trending-products")
    public ResponseEntity<?> getHomepageTrendingProducts(@RequestParam(defaultValue = "12") int limit) {
        try {
            return ResponseEntity.ok(homepageAggregationService.getTrendingProducts(Math.min(limit, 60)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch trending products: " + e.getMessage()));
        }
    }

    @GetMapping("/api/homepage/promocodes")
    public ResponseEntity<?> getHomepagePromoCodes() {
        try {
            return ResponseEntity.ok(homepageAggregationService.getActivePromoCodes());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch promo codes: " + e.getMessage()));
        }
    }

    @GetMapping("/api/homepage/promocodes/{id}")
    public ResponseEntity<?> getHomepagePromoCode(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(homepageAggregationService.getActivePromoCode(id));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(new ErrorResponse("Promo code not found"));
        }
    }

    @GetMapping("/api/homepage/campaigns")
    public ResponseEntity<?> getHomepageCampaigns() {
        try {
            return ResponseEntity.ok(homepageAggregationService.getActiveCampaigns());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch campaigns: " + e.getMessage()));
        }
    }

    @GetMapping("/api/homepage/campaigns/{id}")
    public ResponseEntity<?> getHomepageCampaign(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(homepageAggregationService.getActiveCampaign(id));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(new ErrorResponse("Campaign not found"));
        }
    }

    @GetMapping("/api/products/best-sellers")
    public ResponseEntity<?> getBestSellers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = pageable(page, size);
            Page<BestSellerDTO> result = bestSellerService.getBestSellers(pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch best sellers: " + e.getMessage()));
        }
    }

    @GetMapping("/api/products/best-sellers/top")
    public ResponseEntity<?> getTopBestSellers(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<BestSellerDTO> result = bestSellerService.getTopBestSellers(Math.min(limit, 100));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch top best sellers: " + e.getMessage()));
        }
    }

    @GetMapping("/api/products/top-rated")
    public ResponseEntity<?> getTopRated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = pageable(page, size);
            Page<TopRatedDTO> result = topRatedService.getTopRated(pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch top rated products: " + e.getMessage()));
        }
    }

    @GetMapping("/api/products/top-rated/top")
    public ResponseEntity<?> getTopRatedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<TopRatedDTO> result = topRatedService.getTopRatedProducts(Math.min(limit, 100));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch top rated products: " + e.getMessage()));
        }
    }

    @GetMapping("/api/products/most-wishlisted")
    public ResponseEntity<?> getMostWishlisted(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = pageable(page, size);
            Page<MostWishlistedDTO> result = mostWishlistedService.getMostWishlisted(pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch most wishlisted products: " + e.getMessage()));
        }
    }

    @GetMapping("/api/products/most-wishlisted/top")
    public ResponseEntity<?> getTopMostWishlisted(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<MostWishlistedDTO> result = mostWishlistedService.getTopMostWishlisted(Math.min(limit, 100));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch most wishlisted products: " + e.getMessage()));
        }
    }

    @GetMapping("/api/products/trending")
    public ResponseEntity<?> getTrending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = pageable(page, size);
            Page<TrendingDTO> result = trendingService.getTrending(pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch trending products: " + e.getMessage()));
        }
    }

    @GetMapping("/api/products/trending/top")
    public ResponseEntity<?> getTopTrending(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<TrendingDTO> result = trendingService.getTopTrending(Math.min(limit, 100));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch top trending products: " + e.getMessage()));
        }
    }

    @GetMapping("/api/products/recommendations")
    public ResponseEntity<?> getRecommendations(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String category) {
        try {
            List<RecommendationDTO> result;
            if (category != null && !category.isEmpty()) {
                result = productRecommendationService.getRecommendationsByCategory(category, Math.min(limit, 100));
            } else {
                result = productRecommendationService.getRecommendations(Math.min(limit, 100));
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch recommendations: " + e.getMessage()));
        }
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }
}
