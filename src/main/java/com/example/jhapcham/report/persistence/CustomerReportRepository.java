package com.example.jhapcham.report.persistence;

import com.example.jhapcham.report.domain.CustomerReport;
import com.example.jhapcham.report.domain.CustomerReportReason;
import com.example.jhapcham.report.domain.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerReportRepository extends JpaRepository<CustomerReport, Long> {
    List<CustomerReport> findByCustomerId(Long customerId);
    List<CustomerReport> findByStatus(ReportStatus status);

    long countByStatusIn(Collection<ReportStatus> statuses);

    boolean existsByCustomerIdAndReporterIdAndReasonAndStatusIn(
            Long customerId,
            Long reporterId,
            CustomerReportReason reason,
            Collection<ReportStatus> statuses
    );

    Optional<CustomerReport> findByPublicReferenceId(String publicReferenceId);

    boolean existsByPublicReferenceId(String publicReferenceId);
}
