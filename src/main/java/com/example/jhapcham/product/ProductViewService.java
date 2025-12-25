package com.example.jhapcham.product;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductViewService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductViewRepository productViewRepository;

    // record view for logged in and guest
    @Transactional
    public void recordView(Long productId, Long userId) {
        Product product = productRepository.findById(Objects.requireNonNull(productId, "Product ID cannot be null"))
                .orElseThrow(() -> new RuntimeException("Product not found"));

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }

        ProductView view = ProductView.builder()
                .product(product)
                .user(user)
                .build();

        productViewRepository.save(Objects.requireNonNull(view, "Product view cannot be null"));
    }

    // recent views of a user (for future recommendations)
    public List<ProductView> getRecentViewsForUser(Long userId) {
        User user = userRepository.findById(Objects.requireNonNull(userId, "User ID cannot be null"))
                .orElseThrow(() -> new RuntimeException("User not found"));

        return productViewRepository.findTop50ByUserOrderByViewedAtDesc(user);
    }

    // total views for one product
    public long getTotalViewsForProduct(Long productId) {
        Product product = productRepository.findById(Objects.requireNonNull(productId, "Product ID cannot be null"))
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return productViewRepository.countByProduct(product);
    }

}