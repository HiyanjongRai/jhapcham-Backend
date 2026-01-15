package com.example.jhapcham.campaign;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CampaignResponseDTO {
    private Long id;
    private String name;
    private CampaignType type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private DiscountType discountType;
    private CampaignStatus status;
    private Integer priority;
    private String imagePath;
}
