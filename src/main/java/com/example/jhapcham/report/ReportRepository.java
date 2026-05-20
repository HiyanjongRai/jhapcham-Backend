package com.example.jhapcham.report;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.order.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Collection;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByCustomerOrderByCreatedAtDesc(User customer);
    List<Report> findBySellerOrderByCreatedAtDesc(User seller);
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    List<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatusIn(Collection<ReportStatus> statuses);
    Report findTopByCreatedAtBetweenOrderByIdDesc(java.time.LocalDateTime start, java.time.LocalDateTime end);
    boolean existsByOrderItem(OrderItem orderItem);
}

