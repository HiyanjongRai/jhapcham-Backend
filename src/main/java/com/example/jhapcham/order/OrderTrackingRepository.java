package com.example.jhapcham.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Long> {

    List<OrderTracking> findByOrder_IdOrderByUpdateTimeAsc(Long orderId);

    @Query("""
    select t from OrderTracking t
    join t.order o
    where o.customer.id = :userId
    order by t.updateTime desc
""")
    List<OrderTracking> getAllTrackingForUser(Long userId);


}