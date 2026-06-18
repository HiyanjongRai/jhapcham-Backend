package com.example.jhapcham.report.persistence;

import com.example.jhapcham.report.domain.ProductReport;
import com.example.jhapcham.report.domain.ProductReportReason;
import com.example.jhapcham.report.domain.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface ProductReportRepository extends JpaRepository<ProductReport, Long> {

    @Query("SELECT pr FROM ProductReport pr WHERE pr.product.id = :productId AND pr.reason = :reason AND pr.status IN :statuses")
    Optional<ProductReport> findDuplicateReport(
            @Param("productId") Long productId,
            @Param("reason") ProductReportReason reason,
            @Param("statuses") Collection<ReportStatus> statuses
    );

    List<ProductReport> findByProductId(Long productId);

    List<ProductReport> findByStatus(ReportStatus status);

    long countByStatusIn(Collection<ReportStatus> statuses);

    Optional<ProductReport> findByPublicReferenceId(String publicReferenceId);

    boolean existsByPublicReferenceId(String publicReferenceId);
}
