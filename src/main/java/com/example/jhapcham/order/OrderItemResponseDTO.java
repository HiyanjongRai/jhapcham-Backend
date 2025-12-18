package com.example.jhapcham.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class OrderItemResponseDTO {

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

    private String description;
    private String specification;
    private String features;
    private String storageSpec;
    private String colorOptions;
}
