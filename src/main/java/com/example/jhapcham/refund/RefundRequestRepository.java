package com.example.jhapcham.refund;

import com.example.jhapcham.user.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    Page<RefundRequest> findByCustomerAndDeletedFalseOrderByCreatedAtDesc(User customer, Pageable pageable);

    Page<RefundRequest> findBySellerAndDeletedFalseOrderByCreatedAtDesc(User seller, Pageable pageable);

    Page<RefundRequest> findByStatusAndDeletedFalseOrderByCreatedAtDesc(RefundStatus status, Pageable pageable);

    Page<RefundRequest> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Optional<RefundRequest> findByCustomerAndIdempotencyKey(User customer, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefundRequest r WHERE r.id = :id AND r.deleted = false")
    Optional<RefundRequest> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM RefundRequest r
            JOIN r.lineItems li
            WHERE li.orderItem.id = :orderItemId
              AND r.deleted = false
              AND r.status IN :statuses
            """)
    boolean existsActiveForOrderItem(@Param("orderItemId") Long orderItemId,
                                     @Param("statuses") Collection<RefundStatus> statuses);

    @Query("""
            SELECT COALESCE(SUM(li.quantityRequested), 0)
            FROM RefundLineItem li
            WHERE li.orderItem.id = :orderItemId
              AND li.refundRequest.deleted = false
              AND li.refundRequest.status IN :statuses
            """)
    Long sumRefundedQuantityForOrderItem(@Param("orderItemId") Long orderItemId,
                                         @Param("statuses") Collection<RefundStatus> statuses);

    long countByCustomerAndCreatedAtAfter(User customer, LocalDateTime createdAt);

    long countByCustomerAndStatusAndCreatedAtAfter(User customer, RefundStatus status, LocalDateTime createdAt);

    long countBySellerAndStatusAndCreatedAtAfter(User seller, RefundStatus status, LocalDateTime createdAt);

    @Query("SELECT COUNT(r) FROM RefundRequest r WHERE r.deleted = false AND r.status IN :statuses")
    long countActiveByStatuses(@Param("statuses") Collection<RefundStatus> statuses);

    @Query("SELECT COALESCE(SUM(r.totalRefund), 0) FROM RefundRequest r WHERE r.status = :status")
    java.math.BigDecimal sumTotalRefundByStatus(@Param("status") RefundStatus status);

    @Query("SELECT COALESCE(SUM(r.totalRefund), 0) FROM RefundRequest r WHERE r.order.id = :orderId AND r.status = :status")
    java.math.BigDecimal sumTotalRefundByOrderAndStatus(@Param("orderId") Long orderId, @Param("status") RefundStatus status);

    @Query("""
            SELECT r.reason, COUNT(r), COALESCE(SUM(r.totalRefund), 0)
            FROM RefundRequest r
            WHERE r.deleted = false
            GROUP BY r.reason
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> aggregateByReason();

    @Query("""
            SELECT FUNCTION('to_char', r.createdAt, 'YYYY-MM-DD'), COUNT(r), COALESCE(SUM(r.totalRefund), 0)
            FROM RefundRequest r
            WHERE r.createdAt >= :start AND r.deleted = false
            GROUP BY FUNCTION('to_char', r.createdAt, 'YYYY-MM-DD')
            ORDER BY FUNCTION('to_char', r.createdAt, 'YYYY-MM-DD')
            """)
    List<Object[]> trendSince(@Param("start") LocalDateTime start);

    @Query("""
            SELECT r.seller.id, r.seller.fullName, COUNT(r), COALESCE(SUM(r.totalRefund), 0)
            FROM RefundRequest r
            WHERE r.deleted = false
            GROUP BY r.seller.id, r.seller.fullName
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> sellerRefundSummary(Pageable pageable);

    @Query("""
            SELECT li.productIdSnapshot, li.productNameSnapshot, COUNT(li), COALESCE(SUM(li.totalRefund), 0)
            FROM RefundLineItem li
            WHERE li.refundRequest.deleted = false
            GROUP BY li.productIdSnapshot, li.productNameSnapshot
            ORDER BY COUNT(li) DESC
            """)
    List<Object[]> topRefundedProducts(Pageable pageable);

    @Query("""
            SELECT COALESCE(oi.product.category, 'Unknown'), COUNT(li), COALESCE(SUM(li.totalRefund), 0)
            FROM RefundLineItem li
            JOIN li.orderItem oi
            WHERE li.refundRequest.deleted = false
            GROUP BY COALESCE(oi.product.category, 'Unknown')
            ORDER BY COUNT(li) DESC
            """)
    List<Object[]> categoryRefundSummary(Pageable pageable);
}
