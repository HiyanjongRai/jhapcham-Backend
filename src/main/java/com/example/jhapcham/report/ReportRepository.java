package com.example.jhapcham.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatus(ReportStatus status);

    List<Report> findByType(ReportType type);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Report r " +
            "WHERE (r.type = 'SELLER' AND r.reportedEntityId = :userId) " +
            "OR (r.type = 'PRODUCT' AND r.reportedEntityId IN (SELECT p.id FROM com.example.jhapcham.product.Product p WHERE p.sellerProfile.user.id = :userId))")
    List<Report> findReportsForSeller(Long userId);
}
