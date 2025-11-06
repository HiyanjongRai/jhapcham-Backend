package com.example.jhapcham.order;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderItemDTO {
    private Long productId;
    private String productName;
    private String imagePath;
    private Double unitPrice;
    private int quantity;
    private Double lineTotal;
}
