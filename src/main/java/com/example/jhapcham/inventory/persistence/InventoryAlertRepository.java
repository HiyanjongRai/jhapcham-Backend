package com.example.jhapcham.inventory.persistence;


import com.example.jhapcham.inventory.application.*;
import com.example.jhapcham.inventory.domain.*;
import com.example.jhapcham.inventory.dto.*;
import com.example.jhapcham.inventory.persistence.*;
import com.example.jhapcham.product.domain.Product;
import com.example.jhapcham.seller.domain.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryAlertRepository extends JpaRepository<InventoryAlert, Long> {
    
    List<InventoryAlert> findBySellerAndAcknowledgedFalse(SellerProfile seller);
    
    List<InventoryAlert> findByProduct(Product product);
    
    List<InventoryAlert> findBySeller(SellerProfile seller);
    
    Optional<InventoryAlert> findByProductAndAlertType(Product product, InventoryAlertType type);
    
    List<InventoryAlert> findBySellerOrderByCreatedAtDesc(SellerProfile seller);
}
