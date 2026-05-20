package com.example.jhapcham.loyalty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RewardRuleRequestDTO {
    @NotBlank
    private String name;
    @NotNull
    private RewardRuleType ruleType;
    @NotNull
    private BigDecimal rewardRate;
    private BigDecimal multiplier;
    private String category;
    private Long sellerId;
    private boolean active = true;
    private Integer priority = 100;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
}
