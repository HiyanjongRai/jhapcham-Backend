package com.example.jhapcham.refund;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RefundCreateRequestDTO {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Refund reason is required")
    private RefundReason reason;

    /** Optional — defaults to REFUND_ONLY if omitted */
    private RefundType refundType;

    @NotBlank(message = "Reason details are required")
    @Size(max = 3000, message = "Reason details cannot exceed 3000 characters")
    private String reasonDetails;

    private boolean includeShipping;

    @Size(max = 120, message = "Idempotency key cannot exceed 120 characters")
    private String idempotencyKey;

    @Valid
    @NotEmpty(message = "At least one refund line item is required")
    private List<RefundLineItemRequestDTO> items;
}
