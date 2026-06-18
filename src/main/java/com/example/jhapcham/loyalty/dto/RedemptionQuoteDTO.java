package com.example.jhapcham.loyalty.dto;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RedemptionQuoteDTO {
    private Long requestedPoints;
    private Long approvedPoints;
    private BigDecimal discountAmount;
    private BigDecimal conversionRate;
    private BigDecimal maxRedemptionAmount;
    private BigDecimal minimumOrderAmount;
    private String message;
}
