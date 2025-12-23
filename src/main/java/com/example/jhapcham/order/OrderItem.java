package com.example.jhapcham.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.example.jhapcham.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // link to parent order
    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    // link back to product
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // snapshot fields
    @Column(nullable = false)
    private Long productIdSnapshot;

    @Column(nullable = false)
    private String productNameSnapshot;

    private String brandSnapshot;

    private String imagePathSnapshot;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal lineTotal;

    private String selectedColorSnapshot;
    private String selectedStorageSnapshot;

    private LocalDate manufactureDateSnapshot;
    private LocalDate expiryDateSnapshot;

    @Column(columnDefinition = "TEXT")
    private String productDescriptionSnapshot;

    @Column(columnDefinition = "TEXT")
    private String specificationSnapshot;

    @Column(columnDefinition = "TEXT")
    private String featuresSnapshot;

    @Column(columnDefinition = "TEXT")
    private String storageSpecSnapshot;

    @Column(columnDefinition = "TEXT")
    private String colorOptionsSnapshot;

}