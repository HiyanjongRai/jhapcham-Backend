package com.example.jhapcham.report.persistence;

import com.example.jhapcham.report.domain.ReportModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportModerationLogRepository extends JpaRepository<ReportModerationLog, Long> {
    List<ReportModerationLog> findByReportTypeAndReportId(String reportType, Long reportId);
}
