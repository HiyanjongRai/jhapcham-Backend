package com.example.jhapcham.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProduct(Product product);

    List<ProductVariant> findByProductAndActive(Product product, Boolean active);

    Optional<ProductVariant> findBySku(String sku);

    /**
     * Finds a variant that matches ALL provided attribute value IDs.
     * Uses a count-based approach: the variant must have exactly N matches
     * where N = number of provided attribute value IDs.
     */
    @Query("""
        SELECT v FROM ProductVariant v
        WHERE v.product = :product
          AND (
            SELECT COUNT(vav) FROM VariantAttributeValue vav
            WHERE vav.variant = v
              AND vav.attributeValue.id IN :attrValueIds
          ) = :attrCount
          AND (
            SELECT COUNT(vav) FROM VariantAttributeValue vav
            WHERE vav.variant = v
          ) = :attrCount
    """)
    List<ProductVariant> findByProductAndAttributeValues(
        @Param("product") Product product,
        @Param("attrValueIds") List<Long> attrValueIds,
        @Param("attrCount") long attrCount
    );

    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId AND v.active = true")
    List<ProductVariant> findActiveByProductId(@Param("productId") Long productId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM ProductVariant v WHERE v.price IS NULL AND v.stockQuantity IS NULL")
    int deleteInvalidVariants();

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM variant_attribute_values WHERE variant_id NOT IN (SELECT id FROM product_variants)", nativeQuery = true)
    int deleteOrphanedAttributeMappings();
}
