package com.example.jhapcham.product.model.SearchHistory;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<String>> getUserSearches(@PathVariable Long userId) {
        List<String> keywords = searchHistoryService.getRecentSearches(userId);
        return ResponseEntity.ok(keywords);
    }

    @GetMapping("/details/{userId}")
    public ResponseEntity<List<SearchHistory>> getFullHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(searchHistoryService.getFullHistory(userId));
    }
}
