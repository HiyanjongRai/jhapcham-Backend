package com.example.jhapcham.review.domain;


import com.example.jhapcham.review.application.*;
import com.example.jhapcham.review.domain.*;
import com.example.jhapcham.review.dto.*;
import com.example.jhapcham.review.persistence.*;
import com.example.jhapcham.product.domain.Product;
import com.example.jhapcham.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews",
        indexes = {
                @Index(name = "idx_reviews_product_created", columnList = "product_id,created_at"),
                @Index(name = "idx_reviews_user_created", columnList = "user_id,created_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_review_user_product", columnNames = {"user_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer rating; // 1 to 5

    @Column(columnDefinition = "TEXT")
    private String comment;

    private String imagePath;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
