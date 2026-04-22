package com.example.jhapcham.report.dto;

import com.example.jhapcham.report.ReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReportRequestDTO {
    @NotNull(message = "Order ID is mandatory")
    private Long orderId;

    @NotNull(message = "Order Item ID is mandatory")
    private Long orderItemId;

    @NotNull(message = "Reason is mandatory")
    private ReportReason reason;

    @NotBlank(message = "Description is mandatory")
    private String description;

    private List<String> evidenceUrls;
}
