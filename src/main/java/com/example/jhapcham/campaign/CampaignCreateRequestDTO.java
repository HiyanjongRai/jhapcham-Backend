package com.example.jhapcham.campaign;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CampaignCreateRequestDTO {
    private String name;
    private CampaignType type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private DiscountType discountType;
    private Integer priority;
    private org.springframework.web.multipart.MultipartFile image;
}
