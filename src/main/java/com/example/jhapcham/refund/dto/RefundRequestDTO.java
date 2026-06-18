package com.example.jhapcham.refund.dto;

import com.example.jhapcham.refund.domain.RefundType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RefundRequestDTO {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Refund Type is required")
    private RefundType type;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String description;

    private List<String> fileUrls;

    @NotNull(message = "At least one item must be selected for refund")
    private List<RefundItemRequestDTO> items;
}
