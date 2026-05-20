package com.example.jhapcham.dispute;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResponseDTO {
    private Long id;
    private String reportId;
    private Long orderId;
    private Long initiatedByUserId;
    private String initiatedByUserName;
    private Long otherPartyUserId;
    private String otherPartyUserName;
    private String title;
    private String description;
    private DisputeStatus status;
    private String resolution;
    private String adminNotes;
    private List<String> evidenceFiles;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;
}
