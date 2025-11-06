package com.example.jhapcham.product.model.SearchHistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    // ✅ Fetch recent search keywords for a specific user, ordered by most recent
    @Query("""
        SELECT sh.keyword 
        FROM SearchHistory sh
        WHERE sh.user.id = :userId
        GROUP BY sh.keyword
        ORDER BY MAX(sh.searchedAt) DESC
    """)
    List<String> findRecentKeywordsByUser(@Param("userId") Long userId);

    // ✅ Optional: fetch all search records for user if you need detailed logs
    @Query("SELECT sh FROM SearchHistory sh WHERE sh.user.id = :userId ORDER BY sh.searchedAt DESC")
    List<SearchHistory> findAllByUserId(@Param("userId") Long userId);
}
