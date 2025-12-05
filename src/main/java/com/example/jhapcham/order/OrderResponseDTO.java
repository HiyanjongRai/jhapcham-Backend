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

    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private String deliveryAddress;

    private Double totalPrice;
    private String status;
    private LocalDateTime createdAt;

    private List<OrderItemDTO> items;


}