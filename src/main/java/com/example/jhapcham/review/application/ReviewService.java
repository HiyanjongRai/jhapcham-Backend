package com.example.jhapcham.review.application;


import com.example.jhapcham.review.application.*;
import com.example.jhapcham.review.domain.*;
import com.example.jhapcham.review.dto.*;
import com.example.jhapcham.review.persistence.*;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.activity.domain.ActivityType;
import com.example.jhapcham.activity.application.UserActivityService;
import com.example.jhapcham.common.CloudinaryService;
import com.example.jhapcham.common.FileStorageService;
import com.example.jhapcham.order.persistence.OrderItemRepository;
import com.example.jhapcham.product.domain.Product;
import com.example.jhapcham.product.persistence.ProductRepository;
import com.example.jhapcham.user.domain.User;
import com.example.jhapcham.user.persistence.UserRepository;
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
    private final CloudinaryService cloudinaryService;
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
        validateRating(rating);

        // 3. Verify purchase and delivery
        boolean hasPurchased = orderItemRepository.hasUserPurchasedProduct(userId, productId);
        if (!hasPurchased) {
            throw new BusinessValidationException("You can only review products that you have purchased and received.");
        }

        // 4. Check for existing review
        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BusinessValidationException("You have already reviewed this product.");
        }

        // 5. Handle image (Stored in Cloudinary)
        String imagePath = null;
        if (image != null && !image.isEmpty()) {
            imagePath = cloudinaryService.upload(image, REVIEW_IMAGE_DIR);
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

    @Transactional
    public ReviewResponseDTO updateReview(Long reviewId, Long userId, Integer rating, String comment,
            MultipartFile image) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new BusinessValidationException("You can only update your own reviews.");
        }
        validateRating(rating);

        review.setRating(rating);
        review.setComment(comment);

        if (image != null && !image.isEmpty()) {
            // If it was local, we can leave it or delete it. User said for "undo" (update) use Cloudinary.
            // If it was already in Cloudinary, delete old one.
            if (review.getImagePath() != null && review.getImagePath().contains("cloudinary.com")) {
                cloudinaryService.delete(review.getImagePath());
            }
            String imagePath = cloudinaryService.upload(image, REVIEW_IMAGE_DIR);
            review.setImagePath(imagePath);
        }

        review = reviewRepository.save(review);
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

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new BusinessValidationException("You can only delete your own reviews.");
        }

        // If it was in Cloudinary, delete it
        if (review.getImagePath() != null && review.getImagePath().contains("cloudinary.com")) {
            cloudinaryService.delete(review.getImagePath());
        }

        reviewRepository.delete(review);
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
                .productImage(r.getProduct().getImages() != null && !r.getProduct().getImages().isEmpty()
                    ? r.getProduct().getImages().get(0).getImagePath() : null)
                .createdAt(r.getCreatedAt())
                .build();
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessValidationException("Rating must be between 1 and 5");
        }
    }
}
