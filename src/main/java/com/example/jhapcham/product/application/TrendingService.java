package com.example.jhapcham.product.application;

import com.example.jhapcham.product.dto.TrendingDTO;
import com.example.jhapcham.product.persistence.TrendingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrendingService {

    private final TrendingRepository trendingRepository;

    @Cacheable(cacheNames = "trending", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<TrendingDTO> getTrending(Pageable pageable) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Page<Object[]> results = trendingRepository.findTrending(sevenDaysAgo, pageable);
        List<TrendingDTO> dtos = mapToTrendingDTOs(results.getContent());
        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    @Cacheable(cacheNames = "trending-top", key = "#limit")
    public List<TrendingDTO> getTopTrending(int limit) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Object[]> results = trendingRepository.findTopTrending(sevenDaysAgo, PageRequest.of(0, normalizeLimit(limit)));
        return mapToTrendingDTOs(results);
    }

    private List<TrendingDTO> mapToTrendingDTOs(List<Object[]> results) {
        List<TrendingDTO> dtos = new ArrayList<>();
        for (Object[] row : results) {
            TrendingDTO dto = TrendingDTO.builder()
                    .id((Long) row[0])
                    .name((String) row[3])
                    .slug((String) row[4])
                    .category((String) row[5])
                    .brand((String) row[6])
                    .price(new java.math.BigDecimal(row[7].toString()))
                    .salePrice(row[8] != null ? new java.math.BigDecimal(row[8].toString()) : null)
                    .onSale((Boolean) row[9])
                    .stockQuantity(((Number) row[10]).intValue())
                    .imagePaths(parseImagePaths((String) row[11]))
                    .totalViews(((Number) row[12]).longValue())
                    .recentViews(((Number) row[13]).longValue())
                    .averageRating(((Number) row[14]).doubleValue())
                    .totalReviews(((Number) row[15]).intValue())
                    .sellerFullName((String) row[16])
                    .storeName((String) row[17])
                    .logoImagePath((String) row[18])
                    .trendingScore(((Number) row[19]).doubleValue())
                    .build();
            dtos.add(dto);
        }
        return dtos;
    }

    private List<String> parseImagePaths(String imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(imagePaths.split("\\|"));
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }
}
