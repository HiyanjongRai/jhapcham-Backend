package com.example.jhapcham.report;

import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByCustomerOrderByCreatedAtDesc(User customer);
    List<Report> findBySellerOrderByCreatedAtDesc(User seller);
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
}
