package com.example.jhapcham.cart.persistence;


import com.example.jhapcham.cart.application.*;
import com.example.jhapcham.cart.domain.*;
import com.example.jhapcham.cart.dto.*;
import com.example.jhapcham.cart.persistence.*;
import com.example.jhapcham.product.domain.Product;
import com.example.jhapcham.product.domain.ProductVariant;
import com.example.jhapcham.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUser(User user);

    Optional<CartItem> findByUserAndProduct(User user, Product product);

    /** Find existing cart item for the same user + product + variant */
    @Query("SELECT c FROM CartItem c WHERE c.user = :user AND c.product = :product AND " +
           "((:variant IS NULL AND c.variant IS NULL) OR (c.variant = :variant))")
    Optional<CartItem> findByUserAndProductAndVariant(
        @Param("user") User user,
        @Param("product") Product product,
        @Param("variant") ProductVariant variant
    );

    List<CartItem> findAllByProduct(Product product);

    List<CartItem> findAllByVariant(ProductVariant variant);

    void deleteByProduct(Product product);
}