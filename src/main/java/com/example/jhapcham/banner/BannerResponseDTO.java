package com.example.jhapcham.banner;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BannerResponseDTO {
    private Long id;
    private String title;
    private String subtitle;
    private String description;
    private String discountText;
    private String imageUrl;
    private String mobileImageUrl;
    private String buttonText;
    private String buttonLink;
    private String backgroundColor;
    private String textColor;
    private BannerType bannerType;
    private Boolean active;
    private Integer priority;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double overlayOpacity;
    private TextPosition textPosition;
    private AnimationType animationType;
    private Long clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BannerProductResponseDTO> products;
}
