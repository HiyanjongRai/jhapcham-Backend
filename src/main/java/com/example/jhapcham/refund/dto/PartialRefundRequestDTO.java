package com.example.jhapcham.refund.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PartialRefundRequestDTO {
    @NotNull(message = "Partial refund amount is required")
    private BigDecimal amount;

    private String notes;
}
