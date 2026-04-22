package com.example.jhapcham.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    
    List<ProductVariant> findByProduct(Product product);
    
    List<ProductVariant> findByProductAndActive(Product product, Boolean active);
    
    Optional<ProductVariant> findBySku(String sku);
    
    List<ProductVariant> findByProductAndSize(Product product, String size);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM ProductVariant v WHERE v.product = :product AND " +
           "(:color IS NULL OR v.color = :color) AND " +
           "(:size IS NULL OR v.size = :size) AND " +
           "(:capacity IS NULL OR v.capacity = :capacity)")
    List<ProductVariant> findMatchingVariants(@org.springframework.data.repository.query.Param("product") Product product, 
                                              @org.springframework.data.repository.query.Param("color") String color, 
                                              @org.springframework.data.repository.query.Param("size") String size, 
                                              @org.springframework.data.repository.query.Param("capacity") String capacity);
}
