package com.example.jhapcham.product.model.SearchHistory;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    public void logSearch(Long userId, String keyword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        SearchHistory history = new SearchHistory();
        history.setUser(user);
        history.setKeyword(keyword);
        history.setSearchedAt(LocalDateTime.now());
        searchHistoryRepository.save(history);
    }

    public List<String> getRecentSearches(Long userId) {
        return searchHistoryRepository.findRecentKeywordsByUser(userId);
    }

    // Optional — get full logs
    public List<SearchHistory> getFullHistory(Long userId) {
        return searchHistoryRepository.findAllByUserId(userId);
    }
}
