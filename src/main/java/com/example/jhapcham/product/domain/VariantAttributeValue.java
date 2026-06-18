package com.example.jhapcham.product.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "variant_attribute_values",
        uniqueConstraints = @UniqueConstraint(columnNames = {"variant_id", "attribute_value_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "attribute_value_id")
    private AttributeValue attributeValue;
}
