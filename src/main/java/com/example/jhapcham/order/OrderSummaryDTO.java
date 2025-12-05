package com.example.jhapcham.order;

import com.example.jhapcham.order.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderSummaryDTO {
    private Long orderId;
    private Long customerId;
    private String customerImagePath;
    private LocalDateTime createdAt;
    private OrderStatus status;
    private Double totalPrice;
    private List<OrderItemDTO> items;
    private String customerName;
    private String customerEmail;
    private String customerContact;
    private String fullAddress;
    private Double latitude;
    private Double longitude;

}
