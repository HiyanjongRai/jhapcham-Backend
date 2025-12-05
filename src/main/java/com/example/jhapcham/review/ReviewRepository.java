package com.example.jhapcham.review;

import com.example.jhapcham.product.model.rating.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrder_Id(Long orderId);
    List<Review> findByCustomer_Id(Long userId);


    Optional<Review> findByOrder_Id(Long orderId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRating(Long productId);

    @Query("SELECT COUNT(r.id) FROM Review r WHERE r.product.id = :productId")
    Long getReviewCount(Long productId);

    List<Review> findByProduct_Id(Long productId);
    List<Review> findTop200ByCustomer_IdOrderByCreatedAtDesc(Long customerId);

}
