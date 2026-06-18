package com.example.jhapcham.campaign.dto;


import com.example.jhapcham.campaign.application.*;
import com.example.jhapcham.campaign.domain.*;
import com.example.jhapcham.campaign.dto.*;
import com.example.jhapcham.campaign.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CampaignProductResponseDTO {
    private Long id; // CampaignProduct ID
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private Integer stockLimit;
    private String sellerName;
    private String storeName;
    private Long sellerId;
    private CampaignProductStatus status;
}
