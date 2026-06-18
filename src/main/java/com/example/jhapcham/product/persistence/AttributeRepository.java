package com.example.jhapcham.product.persistence;


import com.example.jhapcham.product.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttributeRepository extends JpaRepository<Attribute, Long> {
    Optional<Attribute> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
