package com.example.jhapcham.review;

import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.activity.ActivityType;
import com.example.jhapcham.activity.UserActivityService;
import com.example.jhapcham.common.FileStorageService;
import com.example.jhapcham.order.OrderItemRepository;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final UserActivityService userActivityService;

    private static final String REVIEW_IMAGE_DIR = "review-images";

    @Transactional
    public ReviewResponseDTO addReview(Long userId, Long productId, Integer rating, String comment,
            MultipartFile image) {
        // 1. Verify user exists
        User user = userRepository.findById(Objects.requireNonNull(userId, "User ID cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Verify product exists
        Product product = productRepository.findById(Objects.requireNonNull(productId, "Product ID cannot be null"))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // 3. Verify purchase and delivery
        boolean hasPurchased = orderItemRepository.hasUserPurchasedProduct(userId, productId);
        if (!hasPurchased) {
            throw new BusinessValidationException("You can only review products that you have purchased and received.");
        }

        // 4. Check for existing review (optional - allow mainly one review per product
        // per user?)
        // Let's allow multiple or block? Usually one review per product.
        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            // For now, maybe allow updating? Or throw?
            // As per request "new controller to write a review", simple add.
            // We can check if we want to block dupes. Let's block for now to be safe.
            // throw new RuntimeException("You have already reviewed this product.");
            // Actually, commented out to be flexible unless requested.
        }

        // 5. Handle image
        String imagePath = null;
        if (image != null && !image.isEmpty()) {
            // save(file, subdir, filename)
            // filename can be review_uid_pid_timestamp
            String filename = "review_" + userId + "_" + productId + "_" + System.currentTimeMillis();
            imagePath = fileStorageService.save(image, REVIEW_IMAGE_DIR, filename);
        }

        // 6. Save
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment);
        review.setImagePath(imagePath);

        review = reviewRepository.save(review);

        // Unified activity logging
        userActivityService.recordActivity(userId, productId, ActivityType.REVIEW, rating + " stars - " + comment);

        return mapToDTO(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProductId(productId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDTO> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private ReviewResponseDTO mapToDTO(Review r) {
        return ReviewResponseDTO.builder()
                .id(r.getId())
                .userName(r.getUser().getFullName())
                .userProfileImage(r.getUser().getProfileImagePath())
                .productId(r.getProduct().getId())
                .productName(r.getProduct().getName())
                .rating(r.getRating())
                .comment(r.getComment())
                .imagePath(r.getImagePath())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
