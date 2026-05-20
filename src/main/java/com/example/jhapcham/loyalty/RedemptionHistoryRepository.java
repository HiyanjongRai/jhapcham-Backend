package com.example.jhapcham.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RedemptionHistoryRepository extends JpaRepository<RedemptionHistory, Long> {
    List<RedemptionHistory> findByOrderIdAndRestoredFalse(Long orderId);
}
