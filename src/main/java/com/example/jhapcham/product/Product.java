package com.example.jhapcham.product;

import com.example.jhapcham.seller.SellerProfile;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_profile_id")
    private SellerProfile sellerProfile;

    @Column(nullable = false, length = 1000)
    @Size(max = 1000)
    private String name;

    @Column(length = 1000)
    @Size(max = 1000)
    private String shortDescription;

    @Column(length = 1000)
    @Size(max = 1000)
    private String description;

    @Column(length = 1000)
    @Size(max = 1000)
    private String category;

    @Column(length = 1000)
    @Size(max = 1000)
    private String brand;

    @Column(length = 1000)
    @Size(max = 1000)
    private String specification;

    @Column(length = 1000)
    @Size(max = 1000)
    private String storageSpec;

    @Column(length = 1000)
    @Size(max = 1000)
    private String features;

    @Column(length = 1000)
    @Size(max = 1000)
    private String colorOptions;

    @Column(nullable = false)
    private BigDecimal price;

    private BigDecimal discountPrice;

    @Column(nullable = false)
    private Integer stockQuantity;

    private Integer warrantyMonths;

    private LocalDate manufactureDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean onSale = false;

    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    public void addImage(ProductImage image) {
        if (images == null) {
            images = new ArrayList<>();
        }
        images.add(image);
        image.setProduct(this);
    }

    private BigDecimal salePercentage;
    private BigDecimal salePrice;
    private Boolean freeShipping;
    private Double insideValleyShipping;
    private Double outsideValleyShipping;
    private Double sellerFreeShippingMinOrder;

    private java.time.LocalDateTime saleEndTime;
    private String saleLabel;

}