package com.example.jhapcham.delivery.dto;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import com.example.jhapcham.order.domain.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryResponseDTO {
    private String trackingId;
    private DeliveryStatus deliveryStatus;
    
    // Customer Info (Operational Data)
    private String customerName;
    private String customerPhone;
    private String destinationAddress;
    private String shippingLocation;
    
    // Order & Payment Details
    private boolean cashOnDelivery;
    private java.math.BigDecimal codAmount;
    private String itemSummary;

    private CourierDTO courier;
    private String notes;
    private LocalDateTime assignedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
