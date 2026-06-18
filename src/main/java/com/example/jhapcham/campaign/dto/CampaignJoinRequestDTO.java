package com.example.jhapcham.campaign.dto;


import com.example.jhapcham.campaign.application.*;
import com.example.jhapcham.campaign.domain.*;
import com.example.jhapcham.campaign.dto.*;
import com.example.jhapcham.campaign.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CampaignJoinRequestDTO {
    private Long campaignId;
    private List<ProductJoinDTO> products;

    @Data
    public static class ProductJoinDTO {
        private Long productId;
        private BigDecimal salePrice;
        private Integer stockLimit;
    }
}
