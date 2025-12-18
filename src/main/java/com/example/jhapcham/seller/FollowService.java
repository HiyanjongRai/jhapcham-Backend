package com.example.jhapcham.seller;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

        private final FollowRepository followRepository;
        private final SellerProfileRepository sellerProfileRepository;
        private final UserRepository userRepository;

        @Transactional
        public String followSeller(Long userId, Long sellerUserId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                SellerProfile seller = sellerProfileRepository.findByUserId(sellerUserId)
                                .orElseThrow(() -> new RuntimeException("Seller not found"));

                if (followRepository.existsByFollowerAndSeller(user, seller)) {
                        return "Already following";
                }

                Follow follow = Follow.builder()
                                .follower(user)
                                .seller(seller)
                                .build();

                followRepository.save(follow);
                return "Followed successfully";
        }

        @Transactional
        public String unfollowSeller(Long userId, Long sellerUserId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                SellerProfile seller = sellerProfileRepository.findByUserId(sellerUserId)
                                .orElseThrow(() -> new RuntimeException("Seller not found"));

                Follow follow = followRepository.findByFollowerAndSeller(user, seller)
                                .orElseThrow(() -> new RuntimeException("Not following"));

                followRepository.delete(follow);
                return "Unfollowed successfully";
        }

        public boolean isFollowing(Long userId, Long sellerUserId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                SellerProfile seller = sellerProfileRepository.findByUserId(sellerUserId)
                                .orElseThrow(() -> new RuntimeException("Seller not found"));

                return followRepository.existsByFollowerAndSeller(user, seller);
        }
}
