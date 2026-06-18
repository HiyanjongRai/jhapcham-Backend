package com.example.jhapcham.product.persistence;


import com.example.jhapcham.product.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
}