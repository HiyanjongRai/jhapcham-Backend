package com.example.jhapcham.order;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

        List<Order> findByUserOrderByCreatedAtDesc(User user);

        List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

        @Query("""
                        SELECT DISTINCT o FROM Order o
                        LEFT JOIN FETCH o.user u
                        LEFT JOIN FETCH o.items i
                        LEFT JOIN FETCH i.product p
                        WHERE p.sellerProfile.user.id = :sellerUserId
                        ORDER BY o.createdAt DESC
                        """)
        List<Order> findOrdersBySeller(@Param("sellerUserId") Long sellerUserId);

        @Query("""
                        SELECT DISTINCT o FROM Order o
                        JOIN o.items i
                        WHERE i.product.sellerProfile.user.id = :sellerUserId
                        AND o.status = :status
                        """)
        List<Order> findOrdersBySellerAndStatus(@Param("sellerUserId") Long sellerUserId,
                        @Param("status") OrderStatus status);

        @Query("""
                        SELECT COUNT(DISTINCT o) FROM Order o
                        JOIN o.items i
                        WHERE i.product.sellerProfile.user.id = :sellerUserId
                        AND o.status = :status
                        """)
        Long countOrdersBySellerAndStatus(@Param("sellerUserId") Long sellerUserId,
                        @Param("status") OrderStatus status);

        @Query("""
                        SELECT DISTINCT o FROM Order o
                        JOIN o.items i
                        WHERE i.product.sellerProfile.user.id = :sellerUserId
                        AND o.createdAt >= :startDate
                        """)
        List<Order> findOrdersBySellerSince(@Param("sellerUserId") Long sellerUserId,
                        @Param("startDate") LocalDateTime startDate);

        boolean existsByItemsProductAndStatusNot(Product product, OrderStatus status);
}