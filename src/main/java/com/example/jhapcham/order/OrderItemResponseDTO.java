package com.example.jhapcham.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class OrderItemResponseDTO {

    private Long id;
    private Long productId;
    private String name;
    private String brand;
    private String imagePath;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private Double commissionRate; // Percentage applied


    private String selectedColor;
    private String selectedStorage;
    private String selectedSize;

    private LocalDate manufactureDate;
    private LocalDate expiryDate;

    private String description;
    private String specification;
    private String features;
    private String storageSpec;
    private String colorOptions;
    private String sellerStoreName;
}
