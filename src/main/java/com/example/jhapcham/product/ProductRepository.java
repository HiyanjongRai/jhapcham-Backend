package com.example.jhapcham.product;

import com.example.jhapcham.seller.SellerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySellerProfile(SellerProfile sellerProfile);

    @Query("""
            SELECT DISTINCT p
            FROM Product p
            LEFT JOIN FETCH p.images
            WHERE p.sellerProfile = :sellerProfile
            """)
    List<Product> findBySellerProfileWithImages(@Param("sellerProfile") SellerProfile sellerProfile);

    List<Product> findBySellerProfileAndStatus(SellerProfile sellerProfile, ProductStatus status);

    Page<Product> findBySellerProfileAndStatus(SellerProfile sellerProfile, ProductStatus status, Pageable pageable);

    List<Product> findByStatus(ProductStatus status);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    List<Product> findByIdInAndStatus(List<Long> ids, ProductStatus status);

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Long countBySellerProfile(SellerProfile sellerProfile);

    Long countBySellerProfileAndStatus(SellerProfile sellerProfile, ProductStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.category FROM Product p WHERE p.status = 'ACTIVE' AND p.category IS NOT NULL")
    List<String> findDistinctCategories();

    /**
     * Find product with pessimistic write lock to prevent race conditions in stock
     * updates.
     * Use this when deducting or restoring stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Query(value = PRODUCT_CARD_SELECT + """
            WHERE p.status = 'ACTIVE'
            ORDER BY p.id DESC
            """,
            countQuery = "SELECT count(*) FROM products p WHERE p.status = 'ACTIVE'",
            nativeQuery = true)
    Page<ProductCardProjection> findActiveProductCards(Pageable pageable);

    @Query(value = PRODUCT_CARD_SELECT + """
            WHERE p.status = 'ACTIVE'
              AND (:category IS NULL OR lower(p.category) = lower(:category))
              AND (:brand IS NULL OR lower(p.brand) LIKE concat('%', lower(:brand), '%'))
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            ORDER BY p.id DESC
            """,
            countQuery = """
                    SELECT count(*) FROM products p
                    WHERE p.status = 'ACTIVE'
                      AND (:category IS NULL OR lower(p.category) = lower(:category))
                      AND (:brand IS NULL OR lower(p.brand) LIKE concat('%', lower(:brand), '%'))
                      AND (:minPrice IS NULL OR p.price >= :minPrice)
                      AND (:maxPrice IS NULL OR p.price <= :maxPrice)
                    """,
            nativeQuery = true)
    Page<ProductCardProjection> filterActiveProductCards(@Param("minPrice") java.math.BigDecimal minPrice,
                                                         @Param("maxPrice") java.math.BigDecimal maxPrice,
                                                         @Param("brand") String brand,
                                                         @Param("category") String category,
                                                         Pageable pageable);

    @Query(value = PRODUCT_CARD_SELECT + """
            WHERE p.status = 'ACTIVE'
              AND (
                :keyword IS NULL OR :keyword = ''
                OR lower(p.name) LIKE concat('%', lower(:keyword), '%')
                OR lower(p.brand) LIKE concat('%', lower(:keyword), '%')
                OR lower(p.short_description) LIKE concat('%', lower(:keyword), '%')
                OR to_tsvector('simple', coalesce(p.name,'') || ' ' || coalesce(p.brand,'') || ' ' || coalesce(p.short_description,'') || ' ' || coalesce(p.description,'') || ' ' || coalesce(p.specification,'') || ' ' || coalesce(p.features,'')) @@ plainto_tsquery('simple', :keyword)
              )
            ORDER BY
              CASE WHEN :keyword IS NULL OR :keyword = '' THEN 0 ELSE ts_rank_cd(
                to_tsvector('simple', coalesce(p.name,'') || ' ' || coalesce(p.brand,'') || ' ' || coalesce(p.short_description,'') || ' ' || coalesce(p.description,'') || ' ' || coalesce(p.specification,'') || ' ' || coalesce(p.features,'')),
                plainto_tsquery('simple', :keyword)
              ) END DESC,
              p.id DESC
            """,
            countQuery = """
                    SELECT count(*) FROM products p
                    WHERE p.status = 'ACTIVE'
                      AND (
                        :keyword IS NULL OR :keyword = ''
                        OR lower(p.name) LIKE concat('%', lower(:keyword), '%')
                        OR lower(p.brand) LIKE concat('%', lower(:keyword), '%')
                        OR lower(p.short_description) LIKE concat('%', lower(:keyword), '%')
                        OR to_tsvector('simple', coalesce(p.name,'') || ' ' || coalesce(p.brand,'') || ' ' || coalesce(p.short_description,'') || ' ' || coalesce(p.description,'') || ' ' || coalesce(p.specification,'') || ' ' || coalesce(p.features,'')) @@ plainto_tsquery('simple', :keyword)
                      )
                    """,
            nativeQuery = true)
    Page<ProductCardProjection> searchActiveProductCards(@Param("keyword") String keyword, Pageable pageable);

    String PRODUCT_CARD_SELECT = """
            SELECT
              p.id AS id,
              p.seller_profile_id AS sellerProfileId,
              u.id AS sellerUserId,
              p.name AS name,
              p.slug AS slug,
              p.short_description AS shortDescription,
              p.description AS description,
              p.category AS category,
              p.brand AS brand,
              p.price AS price,
              p.discount_price AS discountPrice,
              p.sale_percentage AS salePercentage,
              p.sale_price AS salePrice,
              p.sale_label AS saleLabel,
              p.on_sale AS onSale,
              CASE
                WHEN coalesce(p.has_variants, false) THEN coalesce(vs.total_stock, 0)
                ELSE coalesce(p.stock_quantity, 0)
              END AS stockQuantity,
              p.warranty_months AS warrantyMonths,
              p.manufacture_date AS manufactureDate,
              p.expiry_date AS expiryDate,
              p.free_shipping AS freeShipping,
              p.inside_valley_shipping AS insideValleyShipping,
              p.outside_valley_shipping AS outsideValleyShipping,
              p.seller_free_shipping_min_order AS sellerFreeShippingMinOrder,
              p.status AS status,
              imgs.image_paths AS imagePaths,
              coalesce(pv.total_views, 0) AS totalViews,
              rv.average_rating AS averageRating,
              coalesce(rv.total_reviews, 0) AS totalReviews,
              u.full_name AS sellerFullName,
              sp.store_name AS storeName,
              sp.logo_image_path AS logoImagePath,
              u.profile_image_path AS profileImagePath,
              p.sale_end_time AS saleEndTime,
              coalesce(p.has_variants, false) AS hasVariants,
              coalesce(vs.min_price, CASE WHEN p.on_sale = true AND p.sale_price IS NOT NULL THEN p.sale_price ELSE p.price END) AS minPrice,
              coalesce(vs.max_price, CASE WHEN p.on_sale = true AND p.sale_price IS NOT NULL THEN p.sale_price ELSE p.price END) AS maxPrice
            FROM products p
            JOIN seller_profiles sp ON sp.id = p.seller_profile_id
            JOIN users u ON u.id = sp.user_id
            LEFT JOIN (
              SELECT product_id, string_agg(image_path, '|' ORDER BY is_main DESC, sort_order ASC, id ASC) AS image_paths
              FROM product_images
              GROUP BY product_id
            ) imgs ON imgs.product_id = p.id
            LEFT JOIN (
              SELECT product_id, count(*) AS total_views
              FROM product_views
              GROUP BY product_id
            ) pv ON pv.product_id = p.id
            LEFT JOIN (
              SELECT product_id, avg(rating) AS average_rating, count(*) AS total_reviews
              FROM reviews
              GROUP BY product_id
            ) rv ON rv.product_id = p.id
            LEFT JOIN (
              SELECT product_id,
                     sum(coalesce(stock_quantity, 0)) AS total_stock,
                     min(coalesce(price, 0)) FILTER (WHERE active = true) AS min_price,
                     max(coalesce(price, 0)) FILTER (WHERE active = true) AS max_price
              FROM product_variants
              WHERE active = true
              GROUP BY product_id
            ) vs ON vs.product_id = p.id
            """;

}
