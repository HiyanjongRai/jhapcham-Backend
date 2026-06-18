package com.example.jhapcham.analytics.api;

import com.example.jhapcham.analytics.application.PopularSearchService;
import com.example.jhapcham.analytics.dto.PopularSearchDTO;
import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final int MAX_PAGE_SIZE = 60;

    private final PopularSearchService popularSearchService;

    /**
     * GET /api/analytics/popular-searches
     * Retrieve popular search keywords with pagination
     */
    @GetMapping("/popular-searches")
    public ResponseEntity<?> getPopularSearches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = pageable(page, size);
            Page<PopularSearchDTO> result = popularSearchService.getPopularSearches(pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch popular searches: " + e.getMessage()));
        }
    }

    /**
     * GET /api/analytics/popular-searches/top
     * Retrieve top popular search keywords (limited list)
     */
    @GetMapping("/popular-searches/top")
    public ResponseEntity<?> getTopPopularSearches(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<PopularSearchDTO> result = popularSearchService.getTopPopularSearches(Math.min(limit, 100));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch top popular searches: " + e.getMessage()));
        }
    }

    /**
     * GET /api/analytics/popular-searches/recent
     * Retrieve recently searched keywords
     */
    @GetMapping("/popular-searches/recent")
    public ResponseEntity<?> getRecentSearches(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<PopularSearchDTO> result = popularSearchService.getRecentSearches(Math.min(limit, 100));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch recent searches: " + e.getMessage()));
        }
    }

    /**
     * GET /api/analytics/popular-searches/conversion
     * Retrieve searches with highest conversion rates
     */
    @GetMapping("/popular-searches/conversion")
    public ResponseEntity<?> getTopConversionSearches(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<PopularSearchDTO> result = popularSearchService.getTopConversionSearches(Math.min(limit, 100));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch conversion searches: " + e.getMessage()));
        }
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "searchCount"));
    }
}
