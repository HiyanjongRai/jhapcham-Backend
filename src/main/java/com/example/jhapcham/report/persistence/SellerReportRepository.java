package com.example.jhapcham.report.persistence;

import com.example.jhapcham.report.domain.ReportStatus;
import com.example.jhapcham.report.domain.SellerReportReason;
import com.example.jhapcham.report.domain.SellerReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SellerReportRepository extends JpaRepository<SellerReport, Long> {
    List<SellerReport> findBySellerId(Long sellerId);
    List<SellerReport> findByStatus(ReportStatus status);

    long countByStatusIn(Collection<ReportStatus> statuses);

    boolean existsBySellerIdAndReporterIdAndReasonAndStatusIn(
            Long sellerId,
            Long reporterId,
            SellerReportReason reason,
            Collection<ReportStatus> statuses
    );

    Optional<SellerReport> findByPublicReferenceId(String publicReferenceId);

    boolean existsByPublicReferenceId(String publicReferenceId);
}
