package com.example.jhapcham.product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // image belongs to one product
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "image_path", nullable = false, length = 255)
    private String imagePath;

    @Column(name = "is_main", nullable = false)
    private boolean mainImage;

    @Column(name = "sort_order")
    private Integer sortOrder;


}