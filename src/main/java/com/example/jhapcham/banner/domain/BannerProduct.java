package com.example.jhapcham.banner.domain;


import com.example.jhapcham.banner.application.*;
import com.example.jhapcham.banner.domain.*;
import com.example.jhapcham.banner.dto.*;
import com.example.jhapcham.banner.persistence.*;
import com.example.jhapcham.product.domain.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "banner_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Banner banner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer displayOrder;
}
