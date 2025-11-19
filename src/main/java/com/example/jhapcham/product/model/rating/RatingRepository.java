package com.example.jhapcham.product.model.rating;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByProduct_IdAndUserId(Long productId, Long userId);

    Page<Rating> findByProduct_Id(Long productId, Pageable pageable);

    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.product.id = :pid")
    Double averageForProduct(@Param("pid") Long productId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.product.id = :pid")
    long countForProduct(@Param("pid") Long productId);

    @Query("SELECT r FROM Rating r WHERE r.userId = :userId ORDER BY r.createdAt DESC")
    List<Rating> findTop200ByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // ✔ Use correct field name `stars` instead of rating
    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.product.id = :productId")
    Double getAverageRating(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.product.id = :productId")
    Integer countRatings(@Param("productId") Long productId);
}
