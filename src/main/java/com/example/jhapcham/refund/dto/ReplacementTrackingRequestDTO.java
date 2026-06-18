package com.example.jhapcham.refund.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplacementTrackingRequestDTO {

    @NotBlank(message = "Courier name is required")
    private String replacementCourier;

    @NotBlank(message = "Tracking number is required")
    private String replacementTrackingNumber;
}
