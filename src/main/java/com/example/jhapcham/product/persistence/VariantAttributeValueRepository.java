package com.example.jhapcham.product.persistence;


import com.example.jhapcham.product.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VariantAttributeValueRepository extends JpaRepository<VariantAttributeValue, Long> {
    void deleteByVariant(ProductVariant variant);
}
