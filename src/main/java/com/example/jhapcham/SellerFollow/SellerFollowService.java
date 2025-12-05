package com.example.jhapcham.SellerFollow;

import com.example.jhapcham.SellerFollow.FollowedSellerDTO;
import com.example.jhapcham.SellerFollow.SellerFollow;
import com.example.jhapcham.SellerFollow.SellerFollowRepository;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerFollowService {

    private final SellerFollowRepository followRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    public String followSeller(Long customerId, Long sellerId) {

        User customer = userRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        User seller = userRepo.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        boolean exists = followRepo.findByCustomerAndSeller(customer, seller).isPresent();
        if (exists) return "Already following";

        followRepo.save(
                SellerFollow.builder()
                        .customer(customer)
                        .seller(seller)
                        .build()
        );

        return "Followed successfully";
    }

    public List<FollowedSellerDTO> getAllFollowedSellers(Long customerId) {

        User customer = userRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<SellerFollow> follows = followRepo.findAllByCustomer(customer);

        return follows.stream()
                .map(f -> FollowedSellerDTO.builder()
                        .sellerId(f.getSeller().getId())
                        .storeName(f.getSeller().getSellerProfile().getStoreName())
                        .address(f.getSeller().getSellerProfile().getAddress())
                        .logoImagePath(f.getSeller().getSellerProfile().getLogoImagePath())
                        .products(productRepo.findBySellerId(f.getSeller().getId()))
                        .build()
                )
                .collect(Collectors.toList());
    }

    public String unfollowSeller(Long customerId, Long sellerId) {

        User customer = userRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        User seller = userRepo.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        SellerFollow follow = followRepo.findByCustomerAndSeller(customer, seller)
                .orElse(null);

        if (follow == null) return "Not following";

        followRepo.delete(follow);

        return "Unfollowed successfully";
    }


    // ADD THIS METHOD
    public boolean isFollowing(Long customerId, Long sellerId) {

        User customer = userRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        User seller = userRepo.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        return followRepo.findByCustomerAndSeller(customer, seller).isPresent();
    }
}
