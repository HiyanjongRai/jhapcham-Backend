package com.example.jhapcham.product.model.rating;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public Rating addOrUpdateRating(Long productId, Long userId, Double stars) throws Exception {
        if (stars == null || stars < 0.5 || stars > 5.0) {
            throw new Exception("Stars must be between 0.5 and 5.0");
        }

        // Real user from DB
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        // Only CUSTOMER can rate
        if (user.getRole() != Role.CUSTOMER) {
            throw new Exception("Only customers can rate products");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new Exception("Product not found"));

        // Sellers cannot rate their own products
        if (product.getSellerId() != null && Objects.equals(product.getSellerId(), userId)) {
            throw new Exception("Sellers cannot rate their own products");
        }

        Rating rating = ratingRepository.findByProduct_IdAndUserId(productId, userId)
                .orElse(Rating.builder().product(product).userId(userId).build());

        // store with one decimal consistency (no precision/scale on Double)
        rating.setStars(Math.round(stars * 2.0) / 2.0); // snap to 0.5 steps

        Rating saved = ratingRepository.save(rating);
        syncProductAverage(productId);
        return saved;
    }

    public Page<Rating> listRatings(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ratingRepository.findByProduct_Id(productId, pageable);
    }

    @Transactional
    public void deleteRating(Long productId, Long ratingId, Long requesterId, boolean isAdmin) throws Exception {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new Exception("Rating not found"));
        if (!rating.getProduct().getId().equals(productId)) {
            throw new Exception("Rating does not belong to this product");
        }
        // Only owner or admin
        if (!isAdmin && !Objects.equals(rating.getUserId(), requesterId)) {
            throw new Exception("You can delete only your own rating");
        }
        ratingRepository.delete(rating);
        syncProductAverage(productId);
    }

    private void syncProductAverage(Long productId) {
        Double avg = ratingRepository.averageForProduct(productId);
        productRepository.findById(productId).ifPresent(p -> {
            double val = (avg == null) ? 0.0 : Math.round(avg * 10.0) / 10.0;
            p.setRating(val);
            productRepository.save(p);
        });
    }
}
