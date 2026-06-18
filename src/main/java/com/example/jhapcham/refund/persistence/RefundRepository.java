package com.example.jhapcham.refund.persistence;

import com.example.jhapcham.refund.domain.Refund;
import com.example.jhapcham.refund.domain.RefundStatus;
import com.example.jhapcham.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    Optional<Refund> findByRefundNumber(String refundNumber);

    boolean existsByRefundNumber(String refundNumber);

    List<Refund> findByCustomerOrderByCreatedAtDesc(User customer);

    List<Refund> findBySellerOrderByCreatedAtDesc(User seller);

    List<Refund> findByStatusOrderByCreatedAtDesc(RefundStatus status);

    List<Refund> findByStatusInOrderByCreatedAtDesc(List<RefundStatus> statuses);
}
