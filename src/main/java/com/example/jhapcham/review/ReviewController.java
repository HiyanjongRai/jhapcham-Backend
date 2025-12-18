package com.example.jhapcham.review;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // Create a review
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponseDTO> addReview(
            @RequestParam("userId") Long userId,
            @RequestParam("productId") Long productId,
            @RequestParam("rating") Integer rating,
            @RequestParam("comment") String comment,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        ReviewResponseDTO response = reviewService.addReview(userId, productId, rating, comment, image);
        return ResponseEntity.ok(response);
    }

    // Get reviews for a product
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewResponseDTO>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    // Get reviews by a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewResponseDTO>> getUserReviews(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getReviewsByUser(userId));
    }
}
