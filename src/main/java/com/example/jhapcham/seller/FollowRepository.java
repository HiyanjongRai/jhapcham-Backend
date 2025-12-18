package com.example.jhapcham.seller;

import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerAndSeller(User follower, SellerProfile seller);

    boolean existsByFollowerAndSeller(User follower, SellerProfile seller);

    // Custom query if needed for DTOs
    long countBySeller(SellerProfile seller);
}
