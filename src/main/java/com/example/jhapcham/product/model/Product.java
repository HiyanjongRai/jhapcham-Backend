package com.example.jhapcham.product.model;

import com.example.jhapcham.seller.model.SellerProfile;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@Data

@Entity
@Getter

@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 300)
    private String shortDescription;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private String category;

    @Builder.Default
    private Double rating = 0.0;

    @Column(nullable = false)
    private Long sellerId;

    private String imagePath;

    @Column(columnDefinition = "TEXT")
    private String others;

    @Column(length = 150)
    private String brand;

    @Builder.Default
    private int views = 0;

    @Builder.Default
    private int stock = 0;

    @Builder.Default
    private boolean visible = true;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "product_colors", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "color")
    private List<String> colors = new ArrayList<>();

    @Builder.Default
    private boolean onSale = false;

    private Double discountPercent;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> additionalImages = new ArrayList<>();

    private Double salePrice;

    @Column
    private LocalDate manufacturingDate;

    @Column(length = 150)
    private String warranty;

    @Column(length = 1000)
    private String features;

    @Column(length = 1000)
    private String specifications;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "product_storage", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "storage_option")
    private List<String> storage = new ArrayList<>();

    @Column
    private LocalDate expiryDate;

    public enum Status {
        ACTIVE, INACTIVE, DELETED, DRAFT
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id")
    private SellerProfile sellerProfile;

    @Column(nullable = false)
    @Builder.Default
    private Boolean freeShipping = false;


}