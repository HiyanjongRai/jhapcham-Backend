package com.example.jhapcham.order;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
 
        @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product p LEFT JOIN FETCH p.sellerProfile ORDER BY o.createdAt DESC")
        List<Order> findAllWithDetails();


        List<Order> findByUserOrderByCreatedAtDesc(User user);

        List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

        @Query("""
                        SELECT o FROM Order o
                        WHERE (o.user.id = :userId)
                           OR (o.user IS NULL AND LOWER(o.customerEmail) = LOWER(:customerEmail))
                        ORDER BY o.createdAt DESC
                        """)
        List<Order> findForUserOrEmail(@Param("userId") Long userId, @Param("customerEmail") String customerEmail);

        List<Order> findByUserIdAndIdempotencyKeyOrderByCreatedAtAsc(Long userId, String idempotencyKey);

        List<Order> findByUserIdAndIdempotencyKeyStartingWithOrderByCreatedAtAsc(Long userId, String idempotencyKeyPrefix);

        List<Order> findByCustomerEmailIgnoreCaseAndIdempotencyKeyOrderByCreatedAtAsc(String customerEmail,
                        String idempotencyKey);

        List<Order> findByCustomerEmailIgnoreCaseAndIdempotencyKeyStartingWithOrderByCreatedAtAsc(String customerEmail,
                        String idempotencyKeyPrefix);

        @Query("""
                        SELECT o FROM Order o 
                        WHERE o.status <> com.example.jhapcham.order.OrderStatus.DRAFT
                          AND o.id IN (
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
                          AND o.status <> com.example.jhapcham.order.OrderStatus.DRAFT
                        """)
        List<Order> findOrdersBySellerSince(@Param("sellerUserId") Long sellerUserId,
                        @Param("startDate") LocalDateTime startDate);

        List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoff);

        List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

        List<Order> findByCreatedAtAfter(LocalDateTime startDate);

        List<Order> findByStatusAndCreatedAtAfter(OrderStatus status, LocalDateTime startDate);

        @Query("SELECT COALESCE(SUM(o.grandTotal), 0) FROM Order o WHERE o.status = :status")
        java.math.BigDecimal sumGrandTotalByStatus(@Param("status") OrderStatus status);

        @Query("SELECT COALESCE(SUM(o.marketplaceCommission), 0) FROM Order o WHERE o.commissionStatus = :commissionStatus")
        java.math.BigDecimal sumMarketplaceCommissionByCommissionStatus(@Param("commissionStatus") CommissionStatus commissionStatus);

        @Query("""
                        SELECT FUNCTION('to_char', o.createdAt, 'YYYY-MM-DD') as bucket, COUNT(o) as total
                        FROM Order o
                        WHERE o.createdAt >= :startDate
                        GROUP BY FUNCTION('to_char', o.createdAt, 'YYYY-MM-DD')
                        """)
        List<Object[]> countOrdersByDaySince(@Param("startDate") LocalDateTime startDate);

        @Query("""
                        SELECT FUNCTION('to_char', o.createdAt, 'YYYY-MM') as bucket, COALESCE(SUM(o.grandTotal), 0) as total
                        FROM Order o
                        WHERE o.status = :status AND o.createdAt >= :startDate
                        GROUP BY FUNCTION('to_char', o.createdAt, 'YYYY-MM')
                        """)
        List<Object[]> sumRevenueByMonthSince(@Param("status") OrderStatus status,
                        @Param("startDate") LocalDateTime startDate);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT o FROM Order o WHERE o.id IN :ids")
        List<Order> findAllByIdForPaymentUpdate(@Param("ids") Collection<Long> ids);

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

        /**
         * Returns the highest 4-digit numeric sequence already used today.
         *
         * <p>Looks for all {@code customOrderId} values starting with the given
         * {@code dayPrefix} (e.g. {@code "JHC-20260520-"}), strips the prefix,
         * casts the remainder to an integer, and returns the maximum.
         *
         * <p>Returns {@link Optional#empty()} when no orders exist for that prefix
         * (i.e. the first order of the day).
         *
         * @param dayPrefix e.g. {@code "JHC-20260520-"}
         */
        @Query("""
                SELECT MAX(CAST(SUBSTRING(o.customOrderId, LENGTH(:dayPrefix) + 1) AS int))
                FROM Order o
                WHERE o.customOrderId LIKE CONCAT(:dayPrefix, '%')
                """)
        Optional<Integer> findMaxDailySequence(@Param("dayPrefix") String dayPrefix);

        /**
         * Finds an order by its human-readable reference (e.g. {@code JHC-20260520-0003}).
         * Used by the {@code GET /api/orders/ref/{customOrderId}} endpoint.
         */
        Optional<Order> findByCustomOrderId(String customOrderId);
}
