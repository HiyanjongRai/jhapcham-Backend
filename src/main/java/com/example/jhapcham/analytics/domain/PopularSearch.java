package com.example.jhapcham.analytics.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "popular_searches", indexes = {
        @Index(name = "idx_search_count", columnList = "search_count DESC"),
        @Index(name = "idx_last_searched", columnList = "last_searched_at DESC"),
        @Index(name = "idx_keyword", columnList = "search_keyword")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularSearch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String searchKeyword;

    @Column(nullable = false)
    private Long searchCount;

    @Column(nullable = false)
    private Long uniqueUsers;

    @Column(nullable = false)
    private Double conversionRate;

    @Column(nullable = false)
    private LocalDateTime lastSearchedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastSearchedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
