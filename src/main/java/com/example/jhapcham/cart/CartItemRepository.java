package com.example.jhapcham.cart;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductVariant;
import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUser(User user);

    Optional<CartItem> findByUserAndProduct(User user, Product product);

    /** Find existing cart item for the same user + same variant */
    @Query("SELECT c FROM CartItem c WHERE c.user = :user AND c.variant = :variant")
    Optional<CartItem> findByUserAndVariant(
        @Param("user") User user,
        @Param("variant") ProductVariant variant
    );

    List<CartItem> findAllByProduct(Product product);

    List<CartItem> findAllByVariant(ProductVariant variant);
}