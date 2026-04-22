package com.example.jhapcham.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDTO {
    private Long id;
    private String sku;
    private String size;
    private String color;
    private String capacity;
    private String description;
    private Integer stockQuantity;
    private BigDecimal priceModifier;
    private Boolean active;
    private String variantName;
}
