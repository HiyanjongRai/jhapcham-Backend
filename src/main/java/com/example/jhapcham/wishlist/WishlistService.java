package com.example.jhapcham.wishlist;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public void add(Long userId, Long productId) {

        boolean exists = wishlistRepository.existsByUserIdAndProductId(userId, productId);
        if (exists) return;

        User user = userRepository.findById(userId).orElseThrow();
        Product product = productRepository.findById(productId).orElseThrow();

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();

        wishlistRepository.save(item);
    }

    @Transactional
    public void remove(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    public List<WishlistItemDTO> getAll(Long userId) {

        return wishlistRepository.findAllByUserId(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private WishlistItemDTO toDTO(WishlistItem item) {

        Product p = item.getProduct();

        WishlistItemDTO dto = new WishlistItemDTO();

        dto.setId(item.getId());
        dto.setProductId(p.getId());

        dto.setName(p.getName());
        dto.setImagePath(p.getImagePath());
        dto.setPrice(p.getPrice());

        dto.setShortDescription(p.getShortDescription());
        dto.setRating(p.getRating());
        dto.setViews(p.getViews());

        dto.setDiscountPercent(p.getDiscountPercent());
        dto.setSalePrice(p.getSalePrice());



        return dto;
    }
    @Transactional
    public boolean toggle(Long userId, Long productId) {

        boolean exists = wishlistRepository.existsByUserIdAndProductId(userId, productId);

        if (exists) {
            wishlistRepository.deleteByUserIdAndProductId(userId, productId);
            return false;
        }

        User user = userRepository.findById(userId).orElseThrow();
        Product product = productRepository.findById(productId).orElseThrow();

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();

        wishlistRepository.save(item);
        return true;
    }


}