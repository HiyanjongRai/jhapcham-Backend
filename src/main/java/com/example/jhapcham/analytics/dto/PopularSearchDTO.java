package com.example.jhapcham.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularSearchDTO {
    private Long id;
    private String searchKeyword;
    private Long searchCount;
    private Long uniqueUsers;
    private Double conversionRate;
    private java.time.LocalDateTime lastSearchedAt;
    private java.time.LocalDateTime createdAt;
}
