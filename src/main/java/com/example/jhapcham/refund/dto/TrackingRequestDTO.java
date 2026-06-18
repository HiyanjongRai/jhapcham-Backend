package com.example.jhapcham.refund.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TrackingRequestDTO {
    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;
}
