package com.example.jhapcham.product.application;

import com.example.jhapcham.product.dto.TopRatedDTO;
import com.example.jhapcham.product.persistence.TopRatedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopRatedService {

    private final TopRatedRepository topRatedRepository;

    @Cacheable(cacheNames = "top-rated", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<TopRatedDTO> getTopRated(Pageable pageable) {
        Page<Object[]> results = topRatedRepository.findTopRated(pageable);
        List<TopRatedDTO> dtos = mapToTopRatedDTOs(results.getContent());
        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    @Cacheable(cacheNames = "top-rated-top", key = "#limit")
    public List<TopRatedDTO> getTopRatedProducts(int limit) {
        List<Object[]> results = topRatedRepository.findTopRatedProducts(limit);
        return mapToTopRatedDTOs(results);
    }

    private List<TopRatedDTO> mapToTopRatedDTOs(List<Object[]> results) {
        List<TopRatedDTO> dtos = new ArrayList<>();
        for (Object[] row : results) {
            TopRatedDTO dto = TopRatedDTO.builder()
                    .id((Long) row[0])
                    .name((String) row[3])
                    .slug((String) row[4])
                    .category((String) row[5])
                    .brand((String) row[6])
                    .price(new java.math.BigDecimal(row[7].toString()))
                    .salePrice(row[8] != null ? new java.math.BigDecimal(row[8].toString()) : null)
                    .onSale((Boolean) row[9])
                    .hasVariants((Boolean) row[10])
                    .minPrice(row[11] != null ? new java.math.BigDecimal(row[11].toString()) : null)
                    .maxPrice(row[12] != null ? new java.math.BigDecimal(row[12].toString()) : null)
                    .stockQuantity(((Number) row[13]).intValue())
                    .imagePaths(parseImagePaths((String) row[14]))
                    .averageRating(((Number) row[15]).doubleValue())
                    .totalReviews(((Number) row[16]).intValue())
                    .totalViews(((Number) row[17]).longValue())
                    .sellerFullName((String) row[18])
                    .storeName((String) row[19])
                    .logoImagePath((String) row[20])
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
}
