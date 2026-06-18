package com.example.jhapcham.review.persistence;


import com.example.jhapcham.review.application.*;
import com.example.jhapcham.review.domain.*;
import com.example.jhapcham.review.dto.*;
import com.example.jhapcham.review.persistence.*;
import com.example.jhapcham.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductId(Long productId);

    List<Review> findByUserId(Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId); // To prevent duplicate reviews if desired

    @org.springframework.data.jpa.repository.Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double findAverageRatingByProductId(@org.springframework.data.repository.query.Param("productId") Long productId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    Integer countByProductId(@org.springframework.data.repository.query.Param("productId") Long productId);

    void deleteByProduct(Product product);
}
