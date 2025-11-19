package com.example.jhapcham.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrder_Id(Long orderId);
    List<Review> findByCustomer_Id(Long userId);


    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRating(Long productId);

    @Query("SELECT COUNT(r.id) FROM Review r WHERE r.product.id = :productId")
    Long getReviewCount(Long productId);

    List<Review> findByProduct_Id(Long productId);
}
