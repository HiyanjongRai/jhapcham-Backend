package com.example.jhapcham.product.dto;

import com.example.jhapcham.campaign.domain.CampaignStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HomepageCampaignDto {
    private Long id;
    private String campaignName;
    private String banner;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private CampaignStatus status;
}
