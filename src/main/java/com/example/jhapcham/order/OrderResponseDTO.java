package com.example.jhapcham.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponseDTO {

    private Long orderId;
    private LocalDateTime orderDate;
    private String status;

    private String customerName;
    private String customerPhone;
    private String customerAddress;

    private Double subtotal;
    private Double shippingCharge;
    private Double totalAmount;

    private String paymentMethod;
    private String paymentStatus;

    private List<OrderItemDTO> items;

    private Long productId;
    private String name;
    private String brand;
    private String imagePath;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    private String selectedColor;
    private String selectedStorage;

    private LocalDate manufactureDate;
    private LocalDate expiryDate;

}

