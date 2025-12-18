package com.example.jhapcham.order;

import com.example.jhapcham.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    long countByProduct(Product product);

    List<OrderItem> findByProduct(Product product);

    @org.springframework.data.jpa.repository.Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM OrderItem oi JOIN oi.order o WHERE o.user.id = :userId AND oi.product.id = :productId AND o.status = com.example.jhapcham.order.OrderStatus.DELIVERED")
    boolean hasUserPurchasedProduct(@org.springframework.web.bind.annotation.RequestParam("userId") Long userId,
            @org.springframework.web.bind.annotation.RequestParam("productId") Long productId);
}