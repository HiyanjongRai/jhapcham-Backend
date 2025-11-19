package com.example.jhapcham.order;

import com.example.jhapcham.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomer_Id(Long customerId);

    @Query("""
        select distinct o from Order o
        join o.items i
        join i.product p
        where p.sellerId = :sellerId
        order by o.createdAt desc
    """)
    List<Order> findSellerOrders(Long sellerId);
    List<Order> findTop200ByCustomer_IdOrderByCreatedAtDesc(Long customerId);
    List<Order> findByStatus(OrderStatus status);
}
