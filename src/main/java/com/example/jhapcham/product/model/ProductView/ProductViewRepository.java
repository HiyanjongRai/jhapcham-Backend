package com.example.jhapcham.product.model.ProductView;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductViewRepository extends JpaRepository<ProductView, Long> {

    boolean existsByUser_IdAndProduct_IdAndViewedAtBetween(
            Long userId, Long productId, LocalDateTime from, LocalDateTime to);

    boolean existsByAnonKeyAndProduct_IdAndViewedAtBetween(
            String anonKey, Long productId, LocalDateTime from, LocalDateTime to);

    @Query("select count(v) from ProductView v where v.user.id = :userId")
    long countByUserId(Long userId);

    @Query("select count(v) from ProductView v where v.product.id = :productId")
    long countByProductId(Long productId);

    List<ProductView> findByUserIdOrderByViewedAtDesc(Long userId);

    @Query("""
        SELECT v.product.id, COUNT(v)
        FROM ProductView v
        GROUP BY v.product.id
        ORDER BY COUNT(v) DESC
        """)
    List<Object[]> findTopViewedProducts();
}
