package com.example.jhapcham.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderTrackingDTO {
    private Long id;
    private String stage;
    private String message;
    private String branch;
    private LocalDateTime updateTime;
    private Long orderId;

    // ADD THIS
    private List<OrderItemDTO> items;
    private Double totalAmount;
    private LocalDateTime orderDate;
}