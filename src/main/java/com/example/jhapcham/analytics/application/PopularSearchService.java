package com.example.jhapcham.analytics.application;

import com.example.jhapcham.analytics.domain.PopularSearch;
import com.example.jhapcham.analytics.dto.PopularSearchDTO;
import com.example.jhapcham.analytics.persistence.PopularSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularSearchService {

    private final PopularSearchRepository popularSearchRepository;

    @Cacheable(cacheNames = "popular-searches", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<PopularSearchDTO> getPopularSearches(Pageable pageable) {
        Page<PopularSearch> results = popularSearchRepository.findPopularSearches(pageable);
        List<PopularSearchDTO> dtos = results.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    @Cacheable(cacheNames = "popular-searches-top", key = "#limit")
    public List<PopularSearchDTO> getTopPopularSearches(int limit) {
        List<PopularSearch> results = popularSearchRepository.findPopularSearchesByMinCount(10L);
        return results.stream()
                .limit(limit)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "recent-searches", key = "#limit")
    public List<PopularSearchDTO> getRecentSearches(int limit) {
        List<PopularSearch> results = popularSearchRepository.findRecentSearches(PageRequest.of(0, normalizeLimit(limit)));
        return results.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "conversion-searches", key = "#limit")
    public List<PopularSearchDTO> getTopConversionSearches(int limit) {
        List<PopularSearch> results = popularSearchRepository.findTopConversionSearches(PageRequest.of(0, normalizeLimit(limit)));
        return results.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void recordSearch(String keyword) {
        PopularSearch search = popularSearchRepository.findBySearchKeyword(keyword)
                .orElse(PopularSearch.builder()
                        .searchKeyword(keyword)
                        .searchCount(0L)
                        .uniqueUsers(0L)
                        .conversionRate(0.0)
                        .build());
        
        search.setSearchCount(search.getSearchCount() + 1);
        search.setLastSearchedAt(LocalDateTime.now());
        popularSearchRepository.save(search);
    }

    private PopularSearchDTO mapToDTO(PopularSearch search) {
        return PopularSearchDTO.builder()
                .id(search.getId())
                .searchKeyword(search.getSearchKeyword())
                .searchCount(search.getSearchCount())
                .uniqueUsers(search.getUniqueUsers())
                .conversionRate(search.getConversionRate())
                .lastSearchedAt(search.getLastSearchedAt())
                .createdAt(search.getCreatedAt())
                .build();
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }
}
