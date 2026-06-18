package com.example.jhapcham.refund.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutProofRequestDTO {
    @NotBlank(message = "Payment proof screenshot URL is required")
    private String paymentProofUrl;

    @NotBlank(message = "Payment reference/transaction ID is required")
    private String paymentReference;

    private String paymentComment;
}
