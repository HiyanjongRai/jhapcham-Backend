package com.example.jhapcham.inventory;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.seller.SellerProfile;
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
