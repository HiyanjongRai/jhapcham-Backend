package com.example.jhapcham.product.application;

import com.example.jhapcham.product.dto.RecommendationDTO;
import com.example.jhapcham.product.persistence.ProductRepository;
import com.example.jhapcham.Error.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductRecommendationService {

    private final ProductRepository productRepository;

    @Cacheable(cacheNames = "recommendations", key = "#limit")
    public List<RecommendationDTO> getRecommendations(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        var products = productRepository.findActiveProductCards(pageable);
        
        return products.getContent().stream()
                .map(this::mapToRecommendationDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "recommendations-category", key = "#category + '-' + #limit")
    public List<RecommendationDTO> getRecommendationsByCategory(String category, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        var products = productRepository.filterActiveProductCards(null, null, null, category, pageable);
        
        return products.getContent().stream()
                .map(this::mapToRecommendationDTO)
                .collect(Collectors.toList());
    }

    private RecommendationDTO mapToRecommendationDTO(Object projection) {
        // Handle both direct Product objects and projections
        if (projection instanceof com.example.jhapcham.product.persistence.ProductCardProjection) {
            var proj = (com.example.jhapcham.product.persistence.ProductCardProjection) projection;
            return RecommendationDTO.builder()
                    .id(proj.getId())
                    .name(proj.getName())
                    .slug(proj.getSlug())
                    .category(proj.getCategory())
                    .brand(proj.getBrand())
                    .price(proj.getPrice())
                    .salePrice(proj.getSalePrice())
                    .onSale(proj.getOnSale() != null && proj.getOnSale())
                    .stockQuantity(proj.getStockQuantity())
                    .hasVariants(proj.getHasVariants())
                    .minPrice(proj.getMinPrice())
                    .maxPrice(proj.getMaxPrice())
                    .imagePaths(parseImagePaths(proj.getImagePaths()))
                    .averageRating(proj.getAverageRating())
                    .totalReviews(proj.getTotalReviews())
                    .totalViews(proj.getTotalViews())
                    .recommendationReason("Popular in " + proj.getCategory())
                    .sellerFullName(proj.getSellerFullName())
                    .storeName(proj.getStoreName())
                    .logoImagePath(proj.getLogoImagePath())
                    .build();
        }
        throw new BusinessValidationException("Unable to map recommendation data.");
    }

    private List<String> parseImagePaths(String imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(imagePaths.split("\\|"));
    }
}
