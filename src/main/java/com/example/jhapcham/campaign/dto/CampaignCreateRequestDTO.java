package com.example.jhapcham.campaign.dto;

import com.example.jhapcham.campaign.domain.CampaignType;
import com.example.jhapcham.campaign.domain.DiscountType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

@Data
public class CampaignCreateRequestDTO {
    private String name;
    private String description;
    private CampaignType type;
    
    @JsonAlias({"startDate"})
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, 
                pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;
    
    @JsonAlias({"endDate"})
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, 
                pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;
    
    private DiscountType discountType;
    private Double discountValue;
    private Integer maxProducts;
    private Integer priority;
    private String imageUrl;
    
    // File upload support
    private transient MultipartFile image;
}
