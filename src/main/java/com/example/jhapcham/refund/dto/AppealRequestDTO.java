package com.example.jhapcham.refund.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppealRequestDTO {
    @NotBlank(message = "Appeal description/note is required")
    private String notes;
}
