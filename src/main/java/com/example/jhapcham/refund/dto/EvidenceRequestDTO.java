package com.example.jhapcham.refund.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EvidenceRequestDTO {
    @NotBlank(message = "File URL is required")
    private String fileUrl;

    private String note;
}
