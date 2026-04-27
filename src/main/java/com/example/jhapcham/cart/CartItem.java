package com.example.jhapcham.cart;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductVariant;
import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    /**
     * The specific variant the customer selected.
     * This replaces the old selectedColor/selectedStorage/selectedSize strings.
     */
    @ManyToOne
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(nullable = false)
    private Integer quantity;
}