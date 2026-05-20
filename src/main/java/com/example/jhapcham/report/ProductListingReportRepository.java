package com.example.jhapcham.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductListingReportRepository extends JpaRepository<ProductListingReport, Long> {
    
    List<ProductListingReport> findAllByOrderByCreatedAtDesc();
    
    boolean existsByProductIdAndReporterId(Long productId, Long reporterId);
    
    Optional<ProductListingReport> findByReportId(String reportId);

    List<ProductListingReport> findAllByProductSellerProfileUserIdOrderByCreatedAtDesc(Long sellerUserId);
}
