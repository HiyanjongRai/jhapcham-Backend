package com.example.jhapcham.seller;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FollowService {

        private final FollowRepository followRepository;
        private final SellerProfileRepository sellerProfileRepository;
        private final UserRepository userRepository;

        @Transactional
        public String followSeller(Long userId, Long sellerUserId) {
                User user = userRepository.findById(Objects.requireNonNull(userId, "User ID cannot be null"))
                                .orElseThrow(() -> new RuntimeException("User not found"));
                SellerProfile seller = sellerProfileRepository
                                .findByUserId(Objects.requireNonNull(sellerUserId, "Seller user ID cannot be null"))
                                .orElseThrow(() -> new RuntimeException("Seller not found"));

                if (followRepository.existsByFollowerAndSeller(user, seller)) {
                        return "Already following";
                }

                Follow follow = Follow.builder()
                                .follower(user)
                                .seller(seller)
                                .build();

                followRepository.save(Objects.requireNonNull(follow, "Follow object cannot be null"));
                return "Followed successfully";
        }

        @Transactional
        public String unfollowSeller(Long userId, Long sellerUserId) {
                User user = userRepository.findById(Objects.requireNonNull(userId, "User ID cannot be null"))
                                .orElseThrow(() -> new RuntimeException("User not found"));
                SellerProfile seller = sellerProfileRepository
                                .findByUserId(Objects.requireNonNull(sellerUserId, "Seller user ID cannot be null"))
                                .orElseThrow(() -> new RuntimeException("Seller not found"));

                Follow follow = followRepository
                                .findByFollowerAndSeller(user,
                                                Objects.requireNonNull(seller, "Seller profile cannot be null"))
                                .orElseThrow(() -> new RuntimeException("Not following"));

                followRepository.delete(Objects.requireNonNull(follow, "Follow object cannot be null"));
                return "Unfollowed successfully";
        }

        public boolean isFollowing(Long userId, Long sellerUserId) {
                User user = userRepository.findById(Objects.requireNonNull(userId, "User ID cannot be null"))
                                .orElseThrow(() -> new RuntimeException("User not found"));
                SellerProfile seller = sellerProfileRepository
                                .findByUserId(Objects.requireNonNull(sellerUserId, "Seller user ID cannot be null"))
                                .orElseThrow(() -> new RuntimeException("Seller not found"));

                return followRepository.existsByFollowerAndSeller(user, seller);
        }
}
