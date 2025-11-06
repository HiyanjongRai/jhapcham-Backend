package com.example.jhapcham.product.model.comment;

import com.example.jhapcham.product.model.comment.dto.CommentDto;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products/{productId}/comments")
public class CommentController {

    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    // Create: top-level (CUSTOMER only) or reply (CUSTOMER/SELLER)
    @PostMapping
    public ResponseEntity<?> addComment(
            @PathVariable Long productId,
            @RequestParam Long userId,
            @RequestParam String text,
            @RequestParam(required = false) Long parentId
    ) {
        try {
            CommentDto dto = service.create(productId, userId, text, parentId);
            return ResponseEntity.ok(dto); // returns lean DTO (no big product/author blob)
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // List root comments with 1-level replies, paged
    @GetMapping
    public ResponseEntity<?> list(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Page<CommentDto> dto = service.list(productId, page, size);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Delete comment (owner or admin)
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long productId,
            @PathVariable Long commentId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean isAdmin
    ) {
        try {
            service.delete(commentId, userId, isAdmin);
            return ResponseEntity.ok(Map.of("message", "Comment deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PatchMapping("/{commentId}/like")
    public ResponseEntity<?> toggleLike(
            @PathVariable Long productId,
            @PathVariable Long commentId,
            @RequestParam Long userId
    ) {
        try {
            String result = service.toggleLike(commentId, userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Comment " + result + " successfully",
                    "status", result
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
