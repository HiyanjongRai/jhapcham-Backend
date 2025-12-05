package com.example.jhapcham.cart;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.product.model.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "cart_item",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "product_id", "selectedColor", "selectedStorage"}
        )
)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private int quantity;

    @Column(nullable = true)
    private String selectedColor;

    @Column(nullable = true)
    private String selectedStorage;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;
}
