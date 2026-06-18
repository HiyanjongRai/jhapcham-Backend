package com.example.jhapcham.loyalty.dto;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TierConfigRequestDTO {
    @NotNull
    private LoyaltyTier tier;
    @NotNull
    private Long minLifetimePoints;
    @NotNull
    private BigDecimal rewardMultiplier;
    @NotNull
    private String benefits;
    private boolean active = true;
}
