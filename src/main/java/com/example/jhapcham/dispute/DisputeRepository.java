package com.example.jhapcham.dispute;

import com.example.jhapcham.order.Order;
import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    
    List<Dispute> findByInitiatedByUser(User user);
    
    List<Dispute> findByOtherPartyUser(User user);
    
    List<Dispute> findByStatus(DisputeStatus status);
    
    Optional<Dispute> findByOrder(Order order);
    
    List<Dispute> findByInitiatedByUserOrOtherPartyUserOrderByCreatedAtDesc(User user1, User user2);
}
