package com.example.jhapcham.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrackingProductDTO {

    private Long productId;
    private String productName;
    private String imagePath;
    private Integer quantity;
    private Double price;
    private Double lineTotal;
    private String selectedColor;
    private String selectedStorage;
}
