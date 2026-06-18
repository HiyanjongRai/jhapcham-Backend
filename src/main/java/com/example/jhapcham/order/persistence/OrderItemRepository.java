package com.example.jhapcham.order.persistence;


import com.example.jhapcham.order.application.*;
import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.order.dto.*;
import com.example.jhapcham.order.persistence.*;
import com.example.jhapcham.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    long countByProduct(Product product);

    List<OrderItem> findByProduct(Product product);

    @org.springframework.data.jpa.repository.Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM OrderItem oi JOIN oi.order o WHERE o.user.id = :userId AND oi.product.id = :productId AND o.status = 'DELIVERED'")
    boolean hasUserPurchasedProduct(@org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("productId") Long productId);
}