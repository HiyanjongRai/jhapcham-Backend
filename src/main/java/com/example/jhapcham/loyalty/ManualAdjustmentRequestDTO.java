package com.example.jhapcham.loyalty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ManualAdjustmentRequestDTO {
    @NotNull
    private Long customerId;
    @NotNull
    private Long points;
    @NotBlank
    private String reason;
}
