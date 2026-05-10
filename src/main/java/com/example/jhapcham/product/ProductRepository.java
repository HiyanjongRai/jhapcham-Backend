package com.example.jhapcham.product;

import com.example.jhapcham.seller.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySellerProfile(SellerProfile sellerProfile);

    List<Product> findBySellerProfileAndStatus(SellerProfile sellerProfile, ProductStatus status);

    List<Product> findByStatus(ProductStatus status);

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

}
