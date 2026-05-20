package com.example.jhapcham.refund;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefundGatewayConfirmationDTO {
    @NotNull(message = "Gateway is required")
    private RefundGateway gateway;

    @NotBlank(message = "Provider refund reference is required")
    private String providerRefundReference;

    private boolean success;
    private String message;
}
