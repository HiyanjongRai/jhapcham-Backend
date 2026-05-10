package com.example.jhapcham.order;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
 
        @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product p LEFT JOIN FETCH p.sellerProfile ORDER BY o.createdAt DESC")
        List<Order> findAllWithDetails();


        List<Order> findByUserOrderByCreatedAtDesc(User user);

        List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

        List<Order> findByUserIdAndIdempotencyKeyOrderByCreatedAtAsc(Long userId, String idempotencyKey);

        List<Order> findByUserIdAndIdempotencyKeyStartingWithOrderByCreatedAtAsc(Long userId, String idempotencyKeyPrefix);

        List<Order> findByCustomerEmailIgnoreCaseAndIdempotencyKeyOrderByCreatedAtAsc(String customerEmail,
                        String idempotencyKey);

        List<Order> findByCustomerEmailIgnoreCaseAndIdempotencyKeyStartingWithOrderByCreatedAtAsc(String customerEmail,
                        String idempotencyKeyPrefix);

        @Query("""
                        SELECT o FROM Order o 
                        WHERE o.id IN (
                            SELECT DISTINCT i.order.id 
                            FROM OrderItem i 
                            WHERE i.product.sellerProfile.user.id = :sellerUserId
                        )
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

        List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoff);

        List<Order> findByCreatedAtAfter(LocalDateTime startDate);

        List<Order> findByStatusAndCreatedAtAfter(OrderStatus status, LocalDateTime startDate);

        @Query("""
                        SELECT DISTINCT o FROM Order o
                        WHERE o.status IN :statuses
                        AND NOT EXISTS (
                            SELECT s.id FROM Shipment s
                            WHERE s.order = o
                        )
                        """)
        List<Order> findDeliveryStageOrdersWithoutShipment(@Param("statuses") List<OrderStatus> statuses);

        List<Order> findByPaymentStatusAndCreatedAtBefore(PaymentStatus paymentStatus, LocalDateTime cutoff);

        boolean existsByItemsProductAndStatusNot(Product product, OrderStatus status);
}
