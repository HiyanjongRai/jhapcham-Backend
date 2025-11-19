package com.example.jhapcham.review;

import com.example.jhapcham.order.OrderRepository;
import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepo;
    private final ReviewService reviewService;
    private final OrderRepository orderRepo;
    private final UserRepository userRepo;



    /* -----------------------------------------------------------
       1. CHECK IF REVIEW EXISTS
    ------------------------------------------------------------ */
    @GetMapping("/exists")
    public ResponseEntity<?> reviewExists(
            @RequestParam Long orderId,
            @RequestParam Long userId
    ) {
        boolean exists = reviewRepo.existsByOrder_Id(orderId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }



    /* -----------------------------------------------------------
       2. SUBMIT REVIEW
    ------------------------------------------------------------ */
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
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }



    /* -----------------------------------------------------------
       3. GET PRODUCT REVIEWS (Clean DTO)
    ------------------------------------------------------------ */
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getProductReviews(@PathVariable Long productId) {

        var reviews = reviewRepo.findByProduct_Id(productId);

        var dtoList = reviews.stream()
                .map(r -> new ReviewSimpleDTO(
                        r.getId(),
                        r.getRating(),
                        r.getComment(),
                        r.getImages()
                ))
                .toList();

        return ResponseEntity.ok(dtoList);
    }



    /* -----------------------------------------------------------
       4. GET ALL REVIEWS BY USER
    ------------------------------------------------------------ */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserReviews(@PathVariable Long userId) {

        var reviews = reviewRepo.findByCustomer_Id(userId);

        var dtoList = reviews.stream()
                .map(r -> new ReviewDTO(
                        r.getId(),
                        r.getRating(),
                        r.getComment(),
                        r.getImages(),
                        r.getProduct().getId(),
                        r.getProduct().getName(),
                        r.getOrder().getId()
                ))
                .toList();

        return ResponseEntity.ok(dtoList);
    }



    /* -----------------------------------------------------------
       5. EDIT REVIEW
    ------------------------------------------------------------ */
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
            return ResponseEntity.ok(updated);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }



    /* -----------------------------------------------------------
       6. DELETE REVIEW
    ------------------------------------------------------------ */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable Long reviewId) {
        if (!reviewRepo.existsById(reviewId)) {
            return ResponseEntity.status(404).body(Map.of("error", "Review not found"));
        }
        reviewRepo.deleteById(reviewId);
        return ResponseEntity.ok(Map.of("message", "Review deleted"));
    }
}
