package com.example.jhapcham.refund;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RefundEvidenceDTO {
    private Long id;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String description;
    private Long uploadedByUserId;
    private String uploadedByName;
    private LocalDateTime createdAt;
}
