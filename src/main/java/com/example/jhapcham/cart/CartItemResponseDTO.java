package com.example.jhapcham.cart;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class CartItemResponseDTO {
    private Long cartItemId;
    private Long productId;
    private Long variantId;
    private String sku;
    private String name;
    private String brand;
    private String image;
    private Integer quantity;
    private BigDecimal price;
    private Long sellerId;
    private Long sellerProfileId;
    private String sellerStoreName;
    private Boolean freeShipping;
    private Double insideValleyShipping;
    private Double outsideValleyShipping;
    private Double sellerFreeShippingMinOrder;
    private Integer stockQuantity;
    private String variantLabel;
    /** Dynamic attributes: e.g. { "Color": "Red", "Storage": "128GB" } */
    private Map<String, String> variantAttributes;
}
