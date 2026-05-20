package com.example.jhapcham.report.dto;

import com.example.jhapcham.report.ProductListingReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductListingReportRequestDTO {
    
    @NotNull(message = "Reason is mandatory")
    private ProductListingReportReason reason;

    @NotBlank(message = "Description is mandatory")
    private String description;
}
