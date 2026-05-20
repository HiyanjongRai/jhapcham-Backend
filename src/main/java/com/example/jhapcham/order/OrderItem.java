package com.example.jhapcham.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductVariant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order", columnList = "order_id"),
        @Index(name = "idx_order_items_product", columnList = "product_id"),
        @Index(name = "idx_order_items_variant", columnList = "variant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    /**
     * The exact variant ordered.
     * NULL for legacy orders placed before variant system migration.
     */
    @ManyToOne
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    // ── Snapshot fields (never change even if product is deleted/modified) ──

    @Column(nullable = false)
    private Long productIdSnapshot;

    @Column(nullable = false)
    private String productNameSnapshot;

    private String brandSnapshot;
    private String imagePathSnapshot;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = true)
    private BigDecimal vatAmount;

    @Column(nullable = false)
    private BigDecimal lineTotal;

    /** SKU of the ordered variant — snapshot */
    private String variantSkuSnapshot;

    /**
     * JSON snapshot of the variant's dynamic attributes at order time.
     * e.g. '{"Color":"Red","Storage":"128GB"}'
     * Keeps order history intact even if variant attributes change later.
     */
    @Column(columnDefinition = "TEXT")
    private String variantAttributesSnapshot;

    private LocalDate manufactureDateSnapshot;
    private LocalDate expiryDateSnapshot;

    @Column(columnDefinition = "TEXT")
    private String productDescriptionSnapshot;

    @Column(columnDefinition = "TEXT")
    private String specificationSnapshot;

    @Column(columnDefinition = "TEXT")
    private String featuresSnapshot;

    private Double commissionPercentageSnapshot;
    private BigDecimal commissionAmountSnapshot;

    @Column(precision = 38, scale = 2)
    private BigDecimal buyingUnitPriceSnapshot;

    @Column(precision = 38, scale = 2)
    private BigDecimal buyingLineTotalSnapshot;

    @Column(precision = 38, scale = 2)
    private BigDecimal sellingLineTotalSnapshot;

    @Column(precision = 38, scale = 2)
    private BigDecimal inputVatAmountSnapshot;

    @Column(precision = 38, scale = 2)
    private BigDecimal outputVatAmountSnapshot;

    @Column(precision = 38, scale = 2)
    private BigDecimal vatPayableSnapshot;

    @Column(precision = 38, scale = 2)
    private BigDecimal sellerPromoDiscountSnapshot;

    @Column(precision = 38, scale = 2)
    private BigDecimal platformDiscountSnapshot;

    @Column(precision = 38, scale = 2)
    private BigDecimal commissionBaseSnapshot;

    @Column(precision = 38, scale = 2)
    private BigDecimal grossProfitSnapshot;

    @Column(precision = 38, scale = 2)
    private BigDecimal netProfitSnapshot;

    @Column(precision = 38, scale = 2)
    private BigDecimal finalSellerEarningSnapshot;
}
