package com.example.jhapcham.delivery;

import com.example.jhapcham.order.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingResponseDTO {
    private String trackingId;
    private DeliveryStatus deliveryStatus;
    private CourierDTO courier;
    private boolean cashOnDelivery;
    private BigDecimal codAmount;
    private LocalDateTime estimatedDeliveryAt;
    private LocalDateTime deliveredAt;

    @Builder.Default
    private List<TrackingEventDTO> timeline = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrackingEventDTO {
        private DeliveryStatus status;
        private String location;
        private String note;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private String trackingId;
        private DeliveryStatus status;
        private String location;
        private String note;
        private String otp;
        private BigDecimal collectedAmount;
    }
}
