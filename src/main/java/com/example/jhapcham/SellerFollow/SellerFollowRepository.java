package com.example.jhapcham.SellerFollow;

import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerFollowRepository extends JpaRepository<SellerFollow, Long> {

    Optional<SellerFollow> findByCustomerAndSeller(User customer, User seller);

    List<SellerFollow> findAllByCustomer(User customer);

    List<SellerFollow> findAllBySeller(User seller);
}
