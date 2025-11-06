package com.example.jhapcham.product.model.rating;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products/{productId}/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    public ResponseEntity<?> rateProduct(
            @PathVariable Long productId,
            @RequestParam Long userId,
            @RequestParam Double stars
    ) {
        try {
            Rating saved = ratingService.addOrUpdateRating(productId, userId, stars);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping
    public ResponseEntity<Page<Rating>> listRatings(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ratingService.listRatings(productId, page, size));
    }


    @DeleteMapping("/{ratingId}")
    public ResponseEntity<?> deleteRating(
            @PathVariable Long productId,
            @PathVariable Long ratingId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean isAdmin
    ) {
        try {
            ratingService.deleteRating(productId, ratingId, userId, isAdmin);
            return ResponseEntity.ok(Map.of("message", "Rating deleted successfully", "ratingId", ratingId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
