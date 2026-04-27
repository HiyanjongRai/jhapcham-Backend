package com.example.jhapcham.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttributeValueRepository extends JpaRepository<AttributeValue, Long> {
    List<AttributeValue> findByAttribute(Attribute attribute);
    Optional<AttributeValue> findByAttributeAndValueIgnoreCase(Attribute attribute, String value);
    
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM attribute_values WHERE id NOT IN (SELECT attribute_value_id FROM variant_attribute_values)", nativeQuery = true)
    int deleteUnusedValues();
}
