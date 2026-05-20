package com.example.jhapcham.refund;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefundLineItemRequestDTO {
    @NotNull(message = "Order item ID is required")
    private Long orderItemId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Refund quantity must be at least 1")
    private Integer quantity;

    private boolean restockInventory = true;
}
