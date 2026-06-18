package com.example.jhapcham.analytics.application;

import com.example.jhapcham.analytics.domain.PopularSearch;
import com.example.jhapcham.analytics.dto.PopularSearchDTO;
import com.example.jhapcham.analytics.persistence.PopularSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularSearchServiceTest {

    @Mock
    private PopularSearchRepository popularSearchRepository;

    @InjectMocks
    private PopularSearchService popularSearchService;

    private PopularSearch mockSearch;

    @BeforeEach
    void setUp() {
        mockSearch = PopularSearch.builder()
                .id(1L)
                .searchKeyword("laptop")
                .searchCount(1000L)
                .uniqueUsers(500L)
                .conversionRate(0.25)
                .lastSearchedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetPopularSearches_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        List<PopularSearch> searches = new ArrayList<>();
        searches.add(mockSearch);
        Page<PopularSearch> mockPage = new PageImpl<>(searches, pageable, 1);
        
        when(popularSearchRepository.findPopularSearches(any(Pageable.class)))
                .thenReturn(mockPage);

        Page<PopularSearchDTO> result = popularSearchService.getPopularSearches(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("laptop", result.getContent().get(0).getSearchKeyword());
        assertEquals(1000L, result.getContent().get(0).getSearchCount());
    }

    @Test
    void testGetTopPopularSearches_Success() {
        List<PopularSearch> searches = new ArrayList<>();
        searches.add(mockSearch);
        
        when(popularSearchRepository.findPopularSearchesByMinCount(10L))
                .thenReturn(searches);

        List<PopularSearchDTO> result = popularSearchService.getTopPopularSearches(10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("laptop", result.get(0).getSearchKeyword());
    }

    @Test
    void testGetRecentSearches_Success() {
        List<PopularSearch> searches = new ArrayList<>();
        searches.add(mockSearch);
        
        when(popularSearchRepository.findRecentSearches(10))
                .thenReturn(searches);

        List<PopularSearchDTO> result = popularSearchService.getRecentSearches(10);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetTopConversionSearches_Success() {
        List<PopularSearch> searches = new ArrayList<>();
        searches.add(mockSearch);
        
        when(popularSearchRepository.findTopConversionSearches(10))
                .thenReturn(searches);

        List<PopularSearchDTO> result = popularSearchService.getTopConversionSearches(10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0.25, result.get(0).getConversionRate());
    }

    @Test
    void testRecordSearch_NewKeyword() {
        when(popularSearchRepository.findBySearchKeyword(anyString()))
                .thenReturn(Optional.empty());
        when(popularSearchRepository.save(any(PopularSearch.class)))
                .thenReturn(mockSearch);

        popularSearchService.recordSearch("new-keyword");

        assertNotNull(mockSearch);
    }

    @Test
    void testRecordSearch_ExistingKeyword() {
        when(popularSearchRepository.findBySearchKeyword("laptop"))
                .thenReturn(Optional.of(mockSearch));
        when(popularSearchRepository.save(any(PopularSearch.class)))
                .thenReturn(mockSearch);

        popularSearchService.recordSearch("laptop");

        assertEquals(1000L, mockSearch.getSearchCount());
    }
}
