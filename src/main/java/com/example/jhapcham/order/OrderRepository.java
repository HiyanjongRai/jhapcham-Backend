package com.example.jhapcham.order;

import com.example.jhapcham.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    @Query("SELECT o FROM Order o JOIN o.products p WHERE p.sellerId = :sellerId")
    List<Order> findBySellerId(Long sellerId);
}
