package com.example.jhapcham.order;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomer_Id(Long customerId);

    // Fetch orders that include at least one product for a seller
    @Query("""
           select distinct o
           from Order o
           join o.items i
           join i.product p
           where p.sellerId = :sellerId
           order by o.createdAt desc
           """)
    List<Order> findSellerOrders(@Param("sellerId") Long sellerId);
}
