package com.example.jhapcham.product.model.ProductView;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductViewRepository extends JpaRepository<ProductView, Long> {

    // --- check if already viewed recently ---
    boolean existsByUser_IdAndProduct_IdAndViewedAtBetween(
            Long userId, Long productId, LocalDateTime from, LocalDateTime to);

    boolean existsByAnonKeyAndProduct_IdAndViewedAtBetween(
            String anonKey, Long productId, LocalDateTime from, LocalDateTime to);

    // --- count total views ---
    @Query("select count(v) from ProductView v where v.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("select count(v) from ProductView v where v.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);

    // --- raw view records (optional fallback) ---
    List<ProductView> findByUserIdOrderByViewedAtDesc(Long userId);

    // --- global top viewed products ---
    @Query("""
        SELECT v.product.id, COUNT(v)
        FROM ProductView v
        GROUP BY v.product.id
        ORDER BY COUNT(v) DESC
        """)
    List<Object[]> findTopViewedProducts();

    // --- Full chronological history for one user (latest first) ---
    @Query("""
           select new com.example.jhapcham.product.model.ProductView.ProductViewDTO(
               v.product.id, v.product.name, v.product.category, v.viewedAt
           )
           from ProductView v
           where v.user.id = :userId
           order by v.viewedAt desc
           """)
    List<ProductViewDTO> findHistoryByUser(@Param("userId") Long userId);

    // --- Aggregated counts (which products user viewed most) ---
    @Query("""
           select new com.example.jhapcham.product.model.ProductView.ProductViewCountDTO(
               v.product.id, v.product.name, count(v)
           )
           from ProductView v
           where v.user.id = :userId
           group by v.product.id, v.product.name
           order by count(v) desc
           """)
    List<ProductViewCountDTO> findTopByUser(@Param("userId") Long userId);
}
