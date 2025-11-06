package com.example.jhapcham.order;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {
    private Long id;
    private String customerName;
    private Double totalPrice;
    private String status;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;
}
