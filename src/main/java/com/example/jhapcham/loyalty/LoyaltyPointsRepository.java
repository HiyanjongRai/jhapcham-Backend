package com.example.jhapcham.loyalty;

import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyaltyPointsRepository extends JpaRepository<LoyaltyPoints, Long> {
    
    Optional<LoyaltyPoints> findByUser(User user);
    
    Optional<LoyaltyPoints> findByUserId(Long userId);
}
