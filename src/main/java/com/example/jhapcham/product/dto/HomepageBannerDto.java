package com.example.jhapcham.product.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HomepageBannerDto {
    private Long id;
    private String title;
    private String subtitle;
    private String image;
    private String redirectUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
