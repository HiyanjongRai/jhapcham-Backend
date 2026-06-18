package com.example.jhapcham.inventory.dto;


import com.example.jhapcham.inventory.application.*;
import com.example.jhapcham.inventory.domain.*;
import com.example.jhapcham.inventory.dto.*;
import com.example.jhapcham.inventory.persistence.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertDTO {
    private Long id;
    private Long productId;
    private String productName;
    private InventoryAlertType alertType;
    private Integer currentStock;
    private Integer thresholdStock;
    private String message;
    private Boolean acknowledged;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acknowledgedAt;
}
