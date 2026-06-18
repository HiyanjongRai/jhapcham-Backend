package com.example.jhapcham.wishlist.application;


import com.example.jhapcham.wishlist.application.*;
import com.example.jhapcham.wishlist.domain.*;
import com.example.jhapcham.wishlist.persistence.*;
import com.example.jhapcham.activity.domain.ActivityType;
import com.example.jhapcham.activity.application.UserActivityService;
import com.example.jhapcham.product.domain.Product;
import com.example.jhapcham.product.persistence.ProductRepository;
import com.example.jhapcham.product.dto.ProductResponseDTO;
import com.example.jhapcham.product.application.ProductService;
import com.example.jhapcham.user.domain.User;
import com.example.jhapcham.user.persistence.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

        private final WishlistRepository wishlistRepository;
        private final UserRepository userRepository;
        private final ProductRepository productRepository;
        private final ProductService productService;
        private final UserActivityService userActivityService;

        @Transactional
        public void addToWishlist(Long userId, Long productId) {

                User user = userRepository.findById(Objects.requireNonNull(userId, "User ID cannot be null"))
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Product product = productRepository
                                .findById(Objects.requireNonNull(productId, "Product ID cannot be null"))
                                .orElseThrow(() -> new RuntimeException("Product not found"));

                if (wishlistRepository.existsByUserAndProduct(user, product)) {
                        return;
                }

                Wishlist wishlist = Wishlist.builder()
                                .user(user)
                                .product(product)
                                .build();

                wishlistRepository.save(Objects.requireNonNull(wishlist, "Wishlist object cannot be null"));

                // Unified activity logging
                userActivityService.recordActivity(userId, productId, ActivityType.WISHLIST, null);
        }

        @Transactional
        public void removeFromWishlist(Long userId, Long productId) {

                User user = userRepository.findById(Objects.requireNonNull(userId, "User ID cannot be null"))
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Product product = productRepository
                                .findById(Objects.requireNonNull(productId, "Product ID cannot be null"))
                                .orElseThrow(() -> new RuntimeException("Product not found"));

                wishlistRepository.deleteByUserAndProduct(user, product);
        }

        public List<ProductResponseDTO> getWishlist(Long userId) {

                User user = userRepository.findById(Objects.requireNonNull(userId, "User ID cannot be null"))
                                .orElseThrow(() -> new RuntimeException("User not found"));

                List<Wishlist> items = wishlistRepository.findByUser(user);

                List<Long> productIds = items.stream()
                                .filter(w -> w.getProduct() != null)
                                .map(w -> w.getProduct().getId())
                                .distinct()
                                .toList();

                if (productIds.isEmpty()) return new ArrayList<>();

                Map<Long, ProductResponseDTO> activeProductsById = productService.listActiveProductsByIds(productIds)
                                .stream()
                                .collect(Collectors.toMap(ProductResponseDTO::getId, p -> p, (a, b) -> a));

                List<ProductResponseDTO> result = new ArrayList<>();
                for (Wishlist w : items) {
                        if (w.getProduct() == null) continue;
                        ProductResponseDTO product = activeProductsById.get(w.getProduct().getId());
                        if (product != null) {
                                result.add(product);
                        }
                }
                return result;
        }

        public boolean isInWishlist(Long userId, Long productId) {

                User user = userRepository.findById(Objects.requireNonNull(userId, "User ID cannot be null"))
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Product product = productRepository
                                .findById(Objects.requireNonNull(productId, "Product ID cannot be null"))
                                .orElseThrow(() -> new RuntimeException("Product not found"));

                return wishlistRepository.existsByUserAndProduct(user, product);
        }
}
