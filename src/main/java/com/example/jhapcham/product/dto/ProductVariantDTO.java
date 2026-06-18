package com.example.jhapcham.product.dto;


import com.example.jhapcham.product.domain.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDTO {
    private Long id;
    private String sku;
    private BigDecimal price;          // absolute price for this variant
    private Boolean onSale;
    private BigDecimal discountPrice;
    private BigDecimal salePercentage;
    private BigDecimal salePrice;
    private BigDecimal finalPrice;
    private Integer stockQuantity;
    private Boolean active;
    private String variantLabel;       // e.g. "Color: Red, Storage: 128GB"
    // Flat map: { "Color": "Red", "Storage": "128GB" }
    private Map<String, String> attributes;
    // List of attribute value IDs for resolution
    private List<Long> attributeValueIds;

    // Request fields (used when creating/updating a variant)
    public static class CreateRequest {
        public String sku;
        public BigDecimal price;
        public Boolean onSale;
        public BigDecimal discountPrice;
        public BigDecimal salePercentage;
        public Integer stockQuantity;
        public List<Long> attributeValueIds; // IDs of AttributeValue rows to assign
    }
}
