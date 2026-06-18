package com.example.jhapcham.product.persistence;

import com.example.jhapcham.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TrendingRepository extends JpaRepository<Product, Long> {

    @Query(value = """
            SELECT DISTINCT p.id, p.seller_profile_id, u.id AS seller_user_id, p.name, p.slug, 
                   p.category, p.brand, p.price, p.sale_price, p.on_sale, 
                   CASE WHEN coalesce(p.has_variants, false) THEN coalesce(vs.total_stock, 0)
                        ELSE coalesce(p.stock_quantity, 0) END AS stock_quantity,
                   imgs.image_paths, COALESCE(pv.total_views, 0) AS total_views,
                   COALESCE(pv_recent.recent_views, 0) AS recent_views,
                   COALESCE(rv.average_rating, 0) AS average_rating,
                   COALESCE(rv.total_reviews, 0) AS total_reviews,
                   u.full_name AS seller_full_name, sp.store_name, sp.logo_image_path,
                   (COALESCE(pv_recent.recent_views, 0) * 0.7 + COALESCE(rv.average_rating, 0) * 0.3) AS trending_score
            FROM products p
            JOIN seller_profiles sp ON sp.id = p.seller_profile_id
            JOIN users u ON u.id = sp.user_id
            LEFT JOIN (
                SELECT product_id, string_agg(image_path, '|' ORDER BY is_main DESC, sort_order ASC, id ASC) AS image_paths
                FROM product_images
                GROUP BY product_id
            ) imgs ON imgs.product_id = p.id
            LEFT JOIN (
                SELECT product_id, COUNT(*) AS total_views
                FROM product_views
                GROUP BY product_id
            ) pv ON pv.product_id = p.id
            LEFT JOIN (
                SELECT product_id, COUNT(*) AS recent_views
                FROM product_views
                WHERE viewed_at >= :sevenDaysAgo
                GROUP BY product_id
            ) pv_recent ON pv_recent.product_id = p.id
            LEFT JOIN (
                SELECT product_id, AVG(rating) AS average_rating, COUNT(*) AS total_reviews
                FROM reviews
                GROUP BY product_id
            ) rv ON rv.product_id = p.id
            LEFT JOIN (
                SELECT product_id, SUM(COALESCE(stock_quantity, 0)) AS total_stock
                FROM product_variants
                WHERE active = true
                GROUP BY product_id
            ) vs ON vs.product_id = p.id
            WHERE p.status = 'ACTIVE'
            GROUP BY p.id, p.seller_profile_id, u.id, p.name, p.slug, p.category, p.brand, 
                     p.price, p.sale_price, p.on_sale, p.stock_quantity, p.has_variants,
                     imgs.image_paths, pv.total_views, pv_recent.recent_views, rv.average_rating, 
                     rv.total_reviews, u.full_name, sp.store_name, sp.logo_image_path, vs.total_stock
            ORDER BY trending_score DESC, p.id DESC
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT p.id) FROM products p
                    WHERE p.status = 'ACTIVE'
                    """,
            nativeQuery = true)
    Page<Object[]> findTrending(@Param("sevenDaysAgo") LocalDateTime sevenDaysAgo, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT p.id, p.seller_profile_id, u.id AS seller_user_id, p.name, p.slug, 
                   p.category, p.brand, p.price, p.sale_price, p.on_sale, 
                   CASE WHEN coalesce(p.has_variants, false) THEN coalesce(vs.total_stock, 0)
                        ELSE coalesce(p.stock_quantity, 0) END AS stock_quantity,
                   imgs.image_paths, COALESCE(pv.total_views, 0) AS total_views,
                   COALESCE(pv_recent.recent_views, 0) AS recent_views,
                   COALESCE(rv.average_rating, 0) AS average_rating,
                   COALESCE(rv.total_reviews, 0) AS total_reviews,
                   u.full_name AS seller_full_name, sp.store_name, sp.logo_image_path,
                   (COALESCE(pv_recent.recent_views, 0) * 0.7 + COALESCE(rv.average_rating, 0) * 0.3) AS trending_score
            FROM products p
            JOIN seller_profiles sp ON sp.id = p.seller_profile_id
            JOIN users u ON u.id = sp.user_id
            LEFT JOIN (
                SELECT product_id, string_agg(image_path, '|' ORDER BY is_main DESC, sort_order ASC, id ASC) AS image_paths
                FROM product_images
                GROUP BY product_id
            ) imgs ON imgs.product_id = p.id
            LEFT JOIN (
                SELECT product_id, COUNT(*) AS total_views
                FROM product_views
                GROUP BY product_id
            ) pv ON pv.product_id = p.id
            LEFT JOIN (
                SELECT product_id, COUNT(*) AS recent_views
                FROM product_views
                WHERE viewed_at >= :sevenDaysAgo
                GROUP BY product_id
            ) pv_recent ON pv_recent.product_id = p.id
            LEFT JOIN (
                SELECT product_id, AVG(rating) AS average_rating, COUNT(*) AS total_reviews
                FROM reviews
                GROUP BY product_id
            ) rv ON rv.product_id = p.id
            LEFT JOIN (
                SELECT product_id, SUM(COALESCE(stock_quantity, 0)) AS total_stock
                FROM product_variants
                WHERE active = true
                GROUP BY product_id
            ) vs ON vs.product_id = p.id
            WHERE p.status = 'ACTIVE'
            GROUP BY p.id, p.seller_profile_id, u.id, p.name, p.slug, p.category, p.brand, 
                     p.price, p.sale_price, p.on_sale, p.stock_quantity, p.has_variants,
                     imgs.image_paths, pv.total_views, pv_recent.recent_views, rv.average_rating, 
                     rv.total_reviews, u.full_name, sp.store_name, sp.logo_image_path, vs.total_stock
            ORDER BY trending_score DESC, p.id DESC
            LIMIT :limit
            """,
            nativeQuery = true)
    List<Object[]> findTopTrending(@Param("sevenDaysAgo") LocalDateTime sevenDaysAgo, @Param("limit") int limit);
}
