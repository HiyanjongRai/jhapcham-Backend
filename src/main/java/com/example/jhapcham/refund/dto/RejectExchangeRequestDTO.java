package com.example.jhapcham.refund.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectExchangeRequestDTO {

    @NotBlank(message = "Rejection notes are required")
    private String notes;
}
