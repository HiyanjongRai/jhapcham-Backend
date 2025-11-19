package com.example.jhapcham.product.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

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

    @ElementCollection
    @CollectionTable(name = "product_colors", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "color")
    private List<String> colors;

    @Builder.Default
    private boolean onSale = false;

    private Double discountPercent;

    private Double salePrice;

    public enum Status {
        ACTIVE, INACTIVE, DELETED, DRAFT
    }


}