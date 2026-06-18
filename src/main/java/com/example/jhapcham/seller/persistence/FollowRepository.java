package com.example.jhapcham.seller.persistence;


import com.example.jhapcham.seller.application.*;
import com.example.jhapcham.seller.domain.*;
import com.example.jhapcham.seller.dto.*;
import com.example.jhapcham.seller.persistence.*;
import com.example.jhapcham.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerAndSeller(User follower, SellerProfile seller);

    boolean existsByFollowerAndSeller(User follower, SellerProfile seller);
    
    java.util.List<Follow> findByFollower(User follower);

    // Custom query if needed for DTOs
    long countBySeller(SellerProfile seller);
}
