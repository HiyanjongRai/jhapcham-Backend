package com.example.jhapcham.loyalty.dto;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ManualAdjustmentRequestDTO {
    @NotNull
    private Long customerId;
    @NotNull
    private Long points;
    @NotBlank
    private String reason;
}
