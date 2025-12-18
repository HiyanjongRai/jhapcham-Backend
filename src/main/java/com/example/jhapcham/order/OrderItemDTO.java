package com.example.jhapcham.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemDTO {

    private Long productId;
    private String name;
    private String brand;
    private String image;
    private Integer quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal totalPrice;
}

