package com.example.jhapcham.refund.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EvidenceResponseDTO {
    private Long id;
    private String fileUrl;
    private String note;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}
