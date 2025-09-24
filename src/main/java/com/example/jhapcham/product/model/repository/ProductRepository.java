package com.example.jhapcham.product.model.repository;

import com.example.jhapcham.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(String category);
  // ... existing code ...
    List<Product> findBySellerId(String sellerId);

    Page<Product> findByNameContainingIgnoreCaseOrTagsContainingIgnoreCase(String name, String tags, Pageable pageable);
}
