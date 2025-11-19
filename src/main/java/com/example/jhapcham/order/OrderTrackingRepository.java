package com.example.jhapcham.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Long> {

    // For user notifications
    @Query("SELECT t FROM OrderTracking t WHERE t.order.customer.id = :userId ORDER BY t.updateTime DESC")
    List<OrderTracking> getAllTrackingForUser(Long userId);

    // For tracking timeline of a single order
    List<OrderTracking> findByOrder_IdOrderByUpdateTimeAsc(Long orderId);


}