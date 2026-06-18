package com.example.jhapcham.report.persistence;

import com.example.jhapcham.report.domain.ReportAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportAttachmentRepository extends JpaRepository<ReportAttachment, Long> {
}
