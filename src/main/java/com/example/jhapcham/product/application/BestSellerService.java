package com.example.jhapcham.product.application;

import com.example.jhapcham.product.dto.BestSellerDTO;
import com.example.jhapcham.product.persistence.BestSellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BestSellerService {

    private final BestSellerRepository bestSellerRepository;

    @Cacheable(cacheNames = "best-sellers", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<BestSellerDTO> getBestSellers(Pageable pageable) {
        Page<Object[]> results = bestSellerRepository.findBestSellers(pageable);
        List<BestSellerDTO> dtos = mapToBestSellerDTOs(results.getContent());
        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    @Cacheable(cacheNames = "best-sellers-top", key = "#limit")
    public List<BestSellerDTO> getTopBestSellers(int limit) {
        List<Object[]> results = bestSellerRepository.findTopBestSellers(PageRequest.of(0, normalizeLimit(limit)));
        return mapToBestSellerDTOs(results);
    }

    private List<BestSellerDTO> mapToBestSellerDTOs(List<Object[]> results) {
        List<BestSellerDTO> dtos = new ArrayList<>();
        for (Object[] row : results) {
            BestSellerDTO dto = BestSellerDTO.builder()
                    .id((Long) row[0])
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
                    .totalSales(((Number) row[15]).longValue())
                    .averageRating(((Number) row[16]).doubleValue())
                    .totalReviews(((Number) row[17]).intValue())
                    .sellerFullName((String) row[18])
                    .storeName((String) row[19])
                    .logoImagePath((String) row[20])
                    .name((String) row[3])
                    .slug((String) row[4])
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
