package com.example.jhapcham.refund;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundReviewRequestDTO {
    private boolean approved;

    @NotBlank(message = "Review note is required")
    @Size(max = 3000, message = "Review note cannot exceed 3000 characters")
    private String note;
}
