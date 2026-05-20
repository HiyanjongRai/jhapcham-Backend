package com.example.jhapcham.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
public class OrderItemResponseDTO {

    private Long id;
    private Long productId;
    private Long variantId;
    private String sku;
    private String name;
    private String brand;
    private String imagePath;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private Double commissionRate;
    private BigDecimal commissionAmount;
    private BigDecimal buyingUnitPrice;
    private BigDecimal buyingLineTotal;
    private BigDecimal sellingLineTotal;
    private BigDecimal inputVatAmount;
    private BigDecimal outputVatAmount;
    private BigDecimal vatPayableAmount;
    private BigDecimal sellerPromoDiscount;
    private BigDecimal platformDiscount;
    private BigDecimal commissionBase;
    private BigDecimal grossProfit;
    private BigDecimal netProfit;
    private BigDecimal finalSellerEarning;

    /** Dynamic attributes snapshot: e.g. {"Color":"Red","Storage":"128GB"} */
    private Map<String, String> variantAttributes;
    private String variantLabel;

    private LocalDate manufactureDate;
    private LocalDate expiryDate;

    private String description;
    private String specification;
    private String features;
    private String sellerStoreName;
}
