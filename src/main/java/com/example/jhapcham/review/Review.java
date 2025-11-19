package com.example.jhapcham.review;

import com.example.jhapcham.order.Order;
import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.user.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "review",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_review_order_item",
                columnNames = {"order_id"}
        )
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    private User customer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private Integer rating;

    @Column(length = 2000)
    private String comment;

    @ElementCollection
    @CollectionTable(name = "review_images", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url")
    private List<String> images;

    private boolean verifiedPurchase;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
