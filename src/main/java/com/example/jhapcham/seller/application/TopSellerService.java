package com.example.jhapcham.seller.application;

import com.example.jhapcham.seller.dto.TopSellerDTO;
import com.example.jhapcham.seller.persistence.SellerRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopSellerService {

    private static final int DEFAULT_LIMIT = 12;
    private static final int MAX_LIMIT = 60;

    private final SellerRankingRepository sellerRankingRepository;

    public List<TopSellerDTO> getTopSellers(int limit) {
        return sellerRankingRepository.findTopSellersBySoldQuantity(normalizeLimit(limit)).stream()
                .map(TopSellerDTO::from)
                .toList();
    }

    public List<TopSellerDTO> getTopRatedSellers(int limit) {
        return sellerRankingRepository.findTopRatedSellers(normalizeLimit(limit)).stream()
                .map(TopSellerDTO::from)
                .toList();
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
