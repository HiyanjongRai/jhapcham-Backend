package com.example.jhapcham.wishlist;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wishlists",
        indexes = {
                @Index(name = "idx_wishlists_user", columnList = "user_id"),
                @Index(name = "idx_wishlists_product", columnList = "product_id")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_wishlist_user_product", columnNames = { "user_id", "product_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;
}
