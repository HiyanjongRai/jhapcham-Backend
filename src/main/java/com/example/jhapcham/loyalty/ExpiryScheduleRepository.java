package com.example.jhapcham.loyalty;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpiryScheduleRepository extends JpaRepository<ExpirySchedule, Long> {
    List<ExpirySchedule> findTop200ByExpiredFalseAndExpiresAtBeforeOrderByExpiresAtAsc(LocalDateTime now);
    List<ExpirySchedule> findTop200ByExpiredFalseAndNotifiedFalseAndExpiresAtBetweenOrderByExpiresAtAsc(LocalDateTime start, LocalDateTime end);
    List<ExpirySchedule> findByCustomerIdAndExpiredFalseOrderByExpiresAtAsc(Long customerId);
}
