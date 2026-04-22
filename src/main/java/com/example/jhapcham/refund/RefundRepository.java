package com.example.jhapcham.refund;

import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByCustomerOrderByCreatedAtDesc(User customer);
    List<Refund> findBySellerOrderByCreatedAtDesc(User seller);
    List<Refund> findByStatusOrderByCreatedAtDesc(RefundStatus status);
    java.util.Optional<Refund> findByOrderItem(com.example.jhapcham.order.OrderItem item);
    java.util.Optional<Refund> findByOrderItemAndStatus(com.example.jhapcham.order.OrderItem item, RefundStatus status);
    
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Refund r WHERE r.orderItem = :item AND r.status = :status")
    java.util.Optional<Refund> findRefundByOrderItemAndStatus(@org.springframework.data.repository.query.Param("item") com.example.jhapcham.order.OrderItem item, @org.springframework.data.repository.query.Param("status") RefundStatus status);
}
