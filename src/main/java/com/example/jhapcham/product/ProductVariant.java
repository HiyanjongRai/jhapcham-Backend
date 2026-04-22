package com.example.jhapcham.product;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(length = 50)
    private String size;

    @Column(length = 50)
    private String color;

    @Column(length = 100)
    private String capacity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer stockQuantity;

    private BigDecimal priceModifier;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    public BigDecimal getFinalPrice(BigDecimal basePrice) {
        if (priceModifier != null) {
            return basePrice.add(priceModifier);
        }
        return basePrice;
    }

    public String getVariantName() {
        StringBuilder sb = new StringBuilder();
        if (size != null) sb.append("Size: ").append(size).append(" ");
        if (color != null) sb.append("Color: ").append(color).append(" ");
        if (capacity != null) sb.append("Capacity: ").append(capacity);
        return sb.toString().trim();
    }
}
