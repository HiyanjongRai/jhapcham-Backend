// src/main/java/com/example/jhapcham/cart/CartItem.java
package com.example.jhapcham.cart;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.product.model.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    private int quantity;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;
}
