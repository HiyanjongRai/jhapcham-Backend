package com.example.jhapcham.wishlist;

import com.example.jhapcham.activity.ActivityType;
import com.example.jhapcham.activity.UserActivityService;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.product.ProductResponseDTO;
import com.example.jhapcham.product.ProductService;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
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
                                .map(w -> w.getProduct().getId())
                                .distinct()
                                .toList();
                Map<Long, ProductResponseDTO> activeProductsById = productService.listActiveProductsByIds(productIds)
                                .stream()
                                .collect(Collectors.toMap(ProductResponseDTO::getId, p -> p));

                List<ProductResponseDTO> result = new ArrayList<>();
                for (Wishlist w : items) {
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
