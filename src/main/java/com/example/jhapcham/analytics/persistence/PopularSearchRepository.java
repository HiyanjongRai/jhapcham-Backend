package com.example.jhapcham.analytics.persistence;

import com.example.jhapcham.analytics.domain.PopularSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PopularSearchRepository extends JpaRepository<PopularSearch, Long> {

    Optional<PopularSearch> findBySearchKeyword(String searchKeyword);

    @Query("SELECT ps FROM PopularSearch ps ORDER BY ps.searchCount DESC")
    Page<PopularSearch> findPopularSearches(Pageable pageable);

    @Query("SELECT ps FROM PopularSearch ps WHERE ps.searchCount >= :minCount ORDER BY ps.searchCount DESC")
    List<PopularSearch> findPopularSearchesByMinCount(@Param("minCount") Long minCount);

    @Query("SELECT ps FROM PopularSearch ps ORDER BY ps.lastSearchedAt DESC")
    List<PopularSearch> findRecentSearches(Pageable pageable);

    @Query("SELECT ps FROM PopularSearch ps ORDER BY ps.conversionRate DESC")
    List<PopularSearch> findTopConversionSearches(Pageable pageable);
}
