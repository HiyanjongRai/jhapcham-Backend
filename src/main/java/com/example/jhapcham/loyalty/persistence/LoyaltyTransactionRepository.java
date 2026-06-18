package com.example.jhapcham.loyalty.persistence;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {
    boolean existsByReferenceKey(String referenceKey);
    Optional<LoyaltyTransaction> findByReferenceKey(String referenceKey);

    Page<LoyaltyTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    List<LoyaltyTransaction> findByOrderIdAndTransactionTypeIn(Long orderId, Collection<LoyaltyTransactionType> types);

    @Query("""
            SELECT COALESCE(SUM(t.points), 0) FROM LoyaltyTransaction t
            WHERE t.createdAt >= :start AND t.transactionType = :type AND t.status <> 'CANCELLED'
            """)
    Long sumPointsSince(@Param("type") LoyaltyTransactionType type, @Param("start") LocalDateTime start);

    @Query("""
            SELECT FUNCTION('to_char', t.createdAt, 'YYYY-MM-DD'), t.transactionType, COALESCE(SUM(t.points), 0)
            FROM LoyaltyTransaction t
            WHERE t.createdAt >= :start
            GROUP BY FUNCTION('to_char', t.createdAt, 'YYYY-MM-DD'), t.transactionType
            ORDER BY FUNCTION('to_char', t.createdAt, 'YYYY-MM-DD')
            """)
    List<Object[]> dailyTrend(@Param("start") LocalDateTime start);
}
