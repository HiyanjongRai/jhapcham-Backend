package com.example.jhapcham.product.model.repository;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.seller.model.SellerProfile;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findBySellerId(Long sellerId);

    @Query("""
    SELECT p FROM Product p
    WHERE p.visible = true
      AND p.status = com.example.jhapcham.product.model.Product.Status.ACTIVE
      AND (
            LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(p.features) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(p.specifications) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(p.others) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
            LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
""")
    List<Product> searchProducts(@Param("keyword") String keyword);


    List<Product> findByOnSaleTrueAndDiscountPercentGreaterThanEqualAndVisibleIsTrueAndStatus(
            Double discountPercent,
            Product.Status status
    );

    List<Product> findByExpiryDateBetween(LocalDate start, LocalDate end);

    long countBySellerId(Long sellerId);

    List<Product> findBySellerProfile(SellerProfile sellerProfile);


}
