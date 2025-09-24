package com.example.jhapcham.product.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_category", columnList = "category"),
        @Index(name = "idx_products_sku", columnList = "sku", unique = true),
        @Index(name = "idx_products_search_rank", columnList = "searchRank"),
        @Index(name = "idx_products_avg_rating", columnList = "averageRating"),
        @Index(name = "idx_products_views", columnList = "viewsCount"),
        @Index(name = "idx_products_sales", columnList = "salesCount"),
        @Index(name = "idx_products_seller_id", columnList = "seller_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer stock;

    private String category;

    private String brand;

    @Column(nullable = false, unique = true)
    private String sku;

    private String imageUrl;

    private Double discountPrice;

    @Column(nullable = false)
    private Double averageRating = 0.0;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Integer searchRank = 0;

    @Column(nullable = false)
    private Long viewsCount = 0L;

    @Column(nullable = false)
    private Long salesCount = 0L;

    @Column(length = 1000)
    private String tags; // comma-separated keywords
  // ... existing code ...
    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (averageRating == null) averageRating = 0.0;
        if (isActive == null) isActive = true;
        if (searchRank == null) searchRank = 0;
        if (viewsCount == null) viewsCount = 0L;
        if (salesCount == null) salesCount = 0L;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
