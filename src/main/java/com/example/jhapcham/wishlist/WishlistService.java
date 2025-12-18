package com.example.jhapcham.wishlist;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.product.ProductResponseDTO;
import com.example.jhapcham.product.ProductService;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Transactional
    public void addToWishlist(Long userId, Long productId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (wishlistRepository.existsByUserAndProduct(user, product)) {
            return;
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        wishlistRepository.save(wishlist);
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        wishlistRepository.deleteByUserAndProduct(user, product);
    }

    public List<ProductResponseDTO> getWishlist(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Wishlist> items = wishlistRepository.findByUser(user);

        List<ProductResponseDTO> result = new ArrayList<>();
        for (Wishlist w : items) {
            result.add(productService.listAllActiveProducts()
                    .stream()
                    .filter(p -> p.getId().equals(w.getProduct().getId()))
                    .findFirst()
                    .orElse(null));
        }

        result.removeIf(p -> p == null);
        return result;
    }

    public boolean isInWishlist(Long userId, Long productId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return wishlistRepository.existsByUserAndProduct(user, product);
    }
}
