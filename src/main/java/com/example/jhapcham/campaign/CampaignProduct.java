package com.example.jhapcham.campaign;

import com.example.jhapcham.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "campaign_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private BigDecimal salePrice;

    @Column(nullable = false)
    private Integer stockLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignProductStatus status;
}
