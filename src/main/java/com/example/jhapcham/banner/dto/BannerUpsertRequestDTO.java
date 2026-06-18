package com.example.jhapcham.banner.dto;


import com.example.jhapcham.banner.application.*;
import com.example.jhapcham.banner.domain.*;
import com.example.jhapcham.banner.dto.*;
import com.example.jhapcham.banner.persistence.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Data
public class BannerUpsertRequestDTO {
    private String title;
    private String subtitle;
    private String description;
    private String discountText;
    private String imageUrl;
    private String mobileImageUrl;
    private MultipartFile image;
    private MultipartFile mobileImage;
    private String buttonText;
    private String buttonLink;
    private String backgroundColor;
    private String textColor;
    private BannerType bannerType;
    private Boolean active;
    private Integer priority;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;
    private Double overlayOpacity;
    private TextPosition textPosition;
    private AnimationType animationType;
}
