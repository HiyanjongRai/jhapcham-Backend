package com.example.jhapcham.product.model;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 300)                 // NEW: short description for cards
    private String shortDescription;      // <— add this

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

    @Builder.Default
    private int views = 0;

    @Builder.Default
    private int stock = 0;

    @Builder.Default
    private boolean visible = true;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;

    public enum Status { ACTIVE, INACTIVE, DELETED, DRAFT }
}
