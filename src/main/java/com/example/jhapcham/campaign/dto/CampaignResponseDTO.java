package com.example.jhapcham.campaign.dto;


import com.example.jhapcham.campaign.application.*;
import com.example.jhapcham.campaign.domain.*;
import com.example.jhapcham.campaign.dto.*;
import com.example.jhapcham.campaign.persistence.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CampaignResponseDTO {
    private Long id;
    private String name;
    private String description;
    private CampaignType type;
    
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    private DiscountType discountType;
    private Double discountValue;
    private Integer maxProducts;
    private CampaignStatus status;
    private Integer priority;
    private String imagePath;
    private Long totalProducts;
    private Long pendingProducts;
}
