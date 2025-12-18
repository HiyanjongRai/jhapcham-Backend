package com.example.jhapcham.product;

import com.example.jhapcham.seller.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySellerProfile(SellerProfile sellerProfile);

    List<Product> findBySellerProfileAndStatus(SellerProfile sellerProfile, ProductStatus status);

    List<Product> findByStatus(ProductStatus status);

    Long countBySellerProfile(SellerProfile sellerProfile);

    Long countBySellerProfileAndStatus(SellerProfile sellerProfile, ProductStatus status);

}
