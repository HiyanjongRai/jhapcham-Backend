package com.example.jhapcham.review;

import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepo;
    private final ReviewService reviewService;

    /* ----------------------
       SUBMIT REVIEW
    ---------------------- */
    @PostMapping
    public ResponseEntity<?> submitReview(
            @RequestParam Long userId,
            @RequestParam Long orderId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) List<MultipartFile> images
    ) {
        try {
            ReviewRequestDTO dto = new ReviewRequestDTO();
            dto.setOrderId(orderId);
            dto.setRating(rating);
            dto.setComment(comment);
            dto.setImages(images);

            Review saved = reviewService.submitReview(userId, dto);

            // FIX: Return simple map to avoid recursion
            return ResponseEntity.ok(Map.of(
                    "message", "Review submitted successfully",
                    "reviewId", saved.getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /* ----------------------
       GET USER REVIEWS
    ---------------------- */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserReviews(@PathVariable Long userId) {
        List<Review> reviews = reviewRepo.findByCustomer_Id(userId);

        // FIX: Manually map to DTO/Map to break infinite recursion loop
        List<Map<String, Object>> response = reviews.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("rating", r.getRating());
            map.put("comment", r.getComment());
            map.put("images", r.getImages());
            map.put("createdAt", r.getCreatedAt());

            // Include minimal Order info
            if (r.getOrder() != null) {
                Map<String, Object> orderMap = new HashMap<>();
                orderMap.put("id", r.getOrder().getId());
                map.put("order", orderMap);
                map.put("orderId", r.getOrder().getId()); // For easier frontend access
            }

            // Include minimal Product info
            if (r.getProduct() != null) {
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("id", r.getProduct().getId());
                productMap.put("name", r.getProduct().getName());
                productMap.put("imagePath", r.getProduct().getImagePath());
                map.put("product", productMap);
            }

            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /* ----------------------
       GET REVIEW BY ORDER
    ---------------------- */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getReviewByOrder(@PathVariable Long orderId) {
        return reviewRepo.findByOrder_Id(orderId)
                .map(r -> {
                    // FIX: Return simple map
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("rating", r.getRating());
                    map.put("comment", r.getComment());
                    map.put("images", r.getImages());
                    map.put("orderId", r.getOrder().getId());
                    return ResponseEntity.ok((Object) map);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /* ----------------------
       EDIT REVIEW
    ---------------------- */
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> editReview(
            @PathVariable Long reviewId,
            @RequestParam Long userId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) List<MultipartFile> images
    ) {
        try {
            ReviewRequestDTO dto = new ReviewRequestDTO();
            dto.setRating(rating);
            dto.setComment(comment);
            dto.setImages(images);

            Review updated = reviewService.editReview(reviewId, userId, dto);

            // FIX: Return simple map
            return ResponseEntity.ok(Map.of(
                    "message", "Review updated successfully",
                    "reviewId", updated.getId()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /* ----------------------
       DELETE REVIEW
    ---------------------- */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable Long reviewId) {
        if (!reviewRepo.existsById(reviewId))
            return ResponseEntity.status(404).body(Map.of("error", "Review not found"));

        reviewRepo.deleteById(reviewId);
        return ResponseEntity.ok(Map.of("message", "Review deleted"));
    }

    /* ----------------------
       GET PRODUCT REVIEWS WITH REVIEWER INFO
    ---------------------- */
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getProductReviews(@PathVariable Long productId) {

        List<Review> reviews = reviewRepo.findByProduct_Id(productId);

        // This was already using a DTO, so it is safe
        List<ReviewWithUserDTO> dtoList = reviews.stream()
                .map(r -> new ReviewWithUserDTO(
                        r.getId(),
                        r.getRating(),
                        r.getComment(),
                        r.getImages(),
                        r.getCustomer().getFullName(),
                        r.getCustomer().getProfileImagePath() != null
                                ? "uploads/customer-profile/" + r.getCustomer().getProfileImagePath()
                                : null
                ))
                .toList();

        return ResponseEntity.ok(dtoList);
    }
}