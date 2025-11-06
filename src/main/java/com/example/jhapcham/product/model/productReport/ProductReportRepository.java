package com.example.jhapcham.product.model.productReport;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductReportRepository extends JpaRepository<ProductReport, Long> {
    List<ProductReport> findByProduct_Id(Long productId);
    List<ProductReport> findByStatus(ProductReport.Status status);
}
