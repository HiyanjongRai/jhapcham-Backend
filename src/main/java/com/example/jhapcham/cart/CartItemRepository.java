package com.example.jhapcham.cart;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUser(User user);

    Optional<CartItem> findByUserAndProduct(User user, Product product);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM CartItem c WHERE c.user = :user AND c.product = :product " +
           "AND (c.selectedColor = :color OR (c.selectedColor IS NULL AND :color IS NULL)) " +
           "AND (c.selectedStorage = :storage OR (c.selectedStorage IS NULL AND :storage IS NULL)) " +
           "AND (c.selectedSize = :size OR (c.selectedSize IS NULL AND :size IS NULL))")
    Optional<CartItem> findExistingItem(
        @org.springframework.data.repository.query.Param("user") User user, 
        @org.springframework.data.repository.query.Param("product") Product product, 
        @org.springframework.data.repository.query.Param("color") String color, 
        @org.springframework.data.repository.query.Param("storage") String storage,
        @org.springframework.data.repository.query.Param("size") String size
    );

    List<CartItem> findAllByProduct(Product product);
}