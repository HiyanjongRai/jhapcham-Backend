package com.example.jhapcham.product;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, unique = true, length = 150)
    private String sku;

    /**
     * Absolute price for this specific variant.
     * This REPLACES the old "priceModifier" approach.
     * If null, the product's base price is used.
     */
    @Column(nullable = true)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<VariantAttributeValue> attributeValues = new ArrayList<>();

    /**
     * Returns the effective price (variant-specific or falls back to product base price).
     */
    public BigDecimal getEffectivePrice(BigDecimal basePrice) {
        return (price != null) ? price : basePrice;
    }

    /**
     * Returns a human-readable name built from dynamic attributes.
     * e.g. "Color: Red, Storage: 128GB"
     */
    public String getVariantLabel() {
        if (attributeValues == null || attributeValues.isEmpty()) return sku;
        StringBuilder sb = new StringBuilder();
        for (VariantAttributeValue vav : attributeValues) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(vav.getAttributeValue().getAttribute().getName())
              .append(": ")
              .append(vav.getAttributeValue().getValue());
        }
        return sb.toString();
    }
}
