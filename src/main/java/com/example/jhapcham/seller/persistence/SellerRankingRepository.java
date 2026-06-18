package com.example.jhapcham.seller.persistence;

import com.example.jhapcham.seller.domain.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SellerRankingRepository extends JpaRepository<SellerProfile, Long> {

    @Query(value = """
            SELECT sp.user_id AS sellerUserId,
                   u.full_name AS sellerFullName,
                   sp.store_name AS storeName,
                   sp.logo_image_path AS logoImagePath,
                   COALESCE(SUM(oi.quantity), 0) AS soldQuantity,
                   COALESCE(AVG(r.rating), 0) AS averageRating,
                   COUNT(r.id) AS totalReviews
            FROM seller_profiles sp
            JOIN users u ON u.id = sp.user_id
            JOIN products p ON p.seller_profile_id = sp.id
            JOIN order_items oi ON oi.product_id = p.id
            JOIN orders o ON o.id = oi.order_id
            LEFT JOIN reviews r ON r.product_id = p.id
            WHERE o.status = 'DELIVERED'
            GROUP BY sp.user_id, u.full_name, sp.store_name, sp.logo_image_path
            ORDER BY soldQuantity DESC, sp.user_id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<TopSellerProjection> findTopSellersBySoldQuantity(@Param("limit") int limit);

    @Query(value = """
            SELECT sp.user_id AS sellerUserId,
                   u.full_name AS sellerFullName,
                   sp.store_name AS storeName,
                   sp.logo_image_path AS logoImagePath,
                   COALESCE(SUM(oi.quantity), 0) AS soldQuantity,
                   COALESCE(AVG(r.rating), 0) AS averageRating,
                   COUNT(r.id) AS totalReviews
            FROM seller_profiles sp
            JOIN users u ON u.id = sp.user_id
            JOIN products p ON p.seller_profile_id = sp.id
            LEFT JOIN order_items oi ON oi.product_id = p.id
            LEFT JOIN reviews r ON r.product_id = p.id
            WHERE p.status = 'ACTIVE'
            GROUP BY sp.user_id, u.full_name, sp.store_name, sp.logo_image_path
            HAVING COUNT(r.id) > 0
            ORDER BY averageRating DESC, totalReviews DESC, soldQuantity DESC, sp.user_id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<TopSellerProjection> findTopRatedSellers(@Param("limit") int limit);
}
