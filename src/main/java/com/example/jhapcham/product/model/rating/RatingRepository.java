package com.example.jhapcham.product.model.rating;

import com.example.jhapcham.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByProduct_IdAndUserId(Long productId, Long userId);

    Page<Rating> findByProduct_Id(Long productId, Pageable pageable);

    @Query("select avg(r.stars) from Rating r where r.product.id = :pid")
    Double averageForProduct(@Param("pid") Long productId);

    @Query("select count(r) from Rating r where r.product.id = :pid")
    long countForProduct(@Param("pid") Long productId);

    @Query("select r from Rating r where r.userId = :userId order by r.createdAt desc")
    List<Rating> findTop200ByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
