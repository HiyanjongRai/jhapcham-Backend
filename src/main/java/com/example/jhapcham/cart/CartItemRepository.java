package com.example.jhapcham.Cart;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUser(User user);

    Optional<CartItem> findByUserAndProduct(User user, Product product);
    List<CartItem> findAllByProduct(Product product);}