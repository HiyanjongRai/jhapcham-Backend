package com.example.jhapcham.product.persistence;

import com.example.jhapcham.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TopRatedRepository extends JpaRepository<Product, Long> {

    @Query(value = """
            SELECT DISTINCT p.id, p.seller_profile_id, u.id AS seller_user_id, p.name, p.slug, 
                   p.category, p.brand, p.price, p.sale_price, p.on_sale,
                   coalesce(p.has_variants, false) AS has_variants,
                   coalesce(vs.min_price, CASE WHEN p.on_sale = true AND p.sale_price IS NOT NULL THEN p.sale_price ELSE p.price END) AS min_price,
                   coalesce(vs.max_price, CASE WHEN p.on_sale = true AND p.sale_price IS NOT NULL THEN p.sale_price ELSE p.price END) AS max_price,
                   CASE WHEN coalesce(p.has_variants, false) THEN coalesce(vs.total_stock, 0)
                        ELSE coalesce(p.stock_quantity, 0) END AS stock_quantity,
                   imgs.image_paths, COALESCE(rv.average_rating, 0) AS average_rating,
                   COALESCE(rv.total_reviews, 0) AS total_reviews,
                   COALESCE(pv.total_views, 0) AS total_views,
                   u.full_name AS seller_full_name, sp.store_name, sp.logo_image_path
            FROM products p
            JOIN seller_profiles sp ON sp.id = p.seller_profile_id
            JOIN users u ON u.id = sp.user_id
            LEFT JOIN (
                SELECT product_id, string_agg(image_path, '|' ORDER BY is_main DESC, sort_order ASC, id ASC) AS image_paths
                FROM product_images
                GROUP BY product_id
            ) imgs ON imgs.product_id = p.id
            LEFT JOIN (
                SELECT product_id, AVG(rating) AS average_rating, COUNT(*) AS total_reviews
                FROM reviews
                GROUP BY product_id
            ) rv ON rv.product_id = p.id
            LEFT JOIN (
                SELECT product_id, COUNT(*) AS total_views
                FROM product_views
                GROUP BY product_id
            ) pv ON pv.product_id = p.id
            LEFT JOIN (
                SELECT product_id,
                       SUM(COALESCE(stock_quantity, 0)) AS total_stock,
                       MIN(COALESCE(price, 0)) AS min_price,
                       MAX(COALESCE(price, 0)) AS max_price
                FROM product_variants
                WHERE active = true
                GROUP BY product_id
            ) vs ON vs.product_id = p.id
            WHERE p.status = 'ACTIVE' AND COALESCE(rv.average_rating, 0) > 0
            GROUP BY p.id, p.seller_profile_id, u.id, p.name, p.slug, p.category, p.brand, 
                     p.price, p.sale_price, p.on_sale, p.stock_quantity, p.has_variants,
                     imgs.image_paths, rv.average_rating, rv.total_reviews, pv.total_views,
                     u.full_name, sp.store_name, sp.logo_image_path, vs.total_stock, vs.min_price, vs.max_price
            ORDER BY average_rating DESC, total_reviews DESC, p.id DESC
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT p.id) FROM products p
                    LEFT JOIN (
                        SELECT product_id, AVG(rating) AS average_rating
                        FROM reviews
                        GROUP BY product_id
                    ) rv ON rv.product_id = p.id
                    WHERE p.status = 'ACTIVE' AND COALESCE(rv.average_rating, 0) > 0
                    """,
            nativeQuery = true)
    Page<Object[]> findTopRated(Pageable pageable);

    @Query(value = """
            SELECT DISTINCT p.id, p.seller_profile_id, u.id AS seller_user_id, p.name, p.slug, 
                   p.category, p.brand, p.price, p.sale_price, p.on_sale,
                   coalesce(p.has_variants, false) AS has_variants,
                   coalesce(vs.min_price, CASE WHEN p.on_sale = true AND p.sale_price IS NOT NULL THEN p.sale_price ELSE p.price END) AS min_price,
                   coalesce(vs.max_price, CASE WHEN p.on_sale = true AND p.sale_price IS NOT NULL THEN p.sale_price ELSE p.price END) AS max_price,
                   CASE WHEN coalesce(p.has_variants, false) THEN coalesce(vs.total_stock, 0)
                        ELSE coalesce(p.stock_quantity, 0) END AS stock_quantity,
                   imgs.image_paths, COALESCE(rv.average_rating, 0) AS average_rating,
                   COALESCE(rv.total_reviews, 0) AS total_reviews,
                   COALESCE(pv.total_views, 0) AS total_views,
                   u.full_name AS seller_full_name, sp.store_name, sp.logo_image_path
            FROM products p
            JOIN seller_profiles sp ON sp.id = p.seller_profile_id
            JOIN users u ON u.id = sp.user_id
            LEFT JOIN (
                SELECT product_id, string_agg(image_path, '|' ORDER BY is_main DESC, sort_order ASC, id ASC) AS image_paths
                FROM product_images
                GROUP BY product_id
            ) imgs ON imgs.product_id = p.id
            LEFT JOIN (
                SELECT product_id, AVG(rating) AS average_rating, COUNT(*) AS total_reviews
                FROM reviews
                GROUP BY product_id
            ) rv ON rv.product_id = p.id
            LEFT JOIN (
                SELECT product_id, COUNT(*) AS total_views
                FROM product_views
                GROUP BY product_id
            ) pv ON pv.product_id = p.id
            LEFT JOIN (
                SELECT product_id,
                       SUM(COALESCE(stock_quantity, 0)) AS total_stock,
                       MIN(COALESCE(price, 0)) AS min_price,
                       MAX(COALESCE(price, 0)) AS max_price
                FROM product_variants
                WHERE active = true
                GROUP BY product_id
            ) vs ON vs.product_id = p.id
            WHERE p.status = 'ACTIVE' AND COALESCE(rv.average_rating, 0) > 0
            GROUP BY p.id, p.seller_profile_id, u.id, p.name, p.slug, p.category, p.brand, 
                     p.price, p.sale_price, p.on_sale, p.stock_quantity, p.has_variants,
                     imgs.image_paths, rv.average_rating, rv.total_reviews, pv.total_views,
                     u.full_name, sp.store_name, sp.logo_image_path, vs.total_stock, vs.min_price, vs.max_price
            ORDER BY average_rating DESC, total_reviews DESC, p.id DESC
            """,
            nativeQuery = true)
    List<Object[]> findTopRatedProducts(Pageable pageable);
}
