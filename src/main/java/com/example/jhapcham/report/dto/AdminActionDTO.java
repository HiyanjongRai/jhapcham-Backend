package com.example.jhapcham.report.dto;

import com.example.jhapcham.refund.PayerType;
import lombok.Data;

@Data
public class AdminActionDTO {
    private boolean approved;
    private String comment;
    private PayerType payerType; // Only if approved
}
