package com.example.jhapcham.product.model.comment;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.comment.dto.CommentDto;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final CommentLikeRepository commentLikeRepo;


    /**
     * Create top-level comment (CUSTOMER only) or reply (CUSTOMER or SELLER).
     */
    @Transactional
    public CommentDto create(Long productId, Long userId, String text, Long parentId) throws Exception {
        if (text == null || text.isBlank()) throw new Exception("Comment cannot be empty");

        User user = userRepo.findById(userId).orElseThrow(() -> new Exception("User not found"));
        Product product = productRepo.findById(productId).orElseThrow(() -> new Exception("Product not found"));

        Comment parent = null;
        if (parentId == null) {
            // Top-level comment => CUSTOMER only
            if (user.getRole() != Role.CUSTOMER) {
                throw new Exception("Only customers can comment on a product");
            }
        } else {
            // Reply => CUSTOMER or SELLER
            if (user.getRole() != Role.CUSTOMER && user.getRole() != Role.SELLER) {
                throw new Exception("Only customers or sellers can reply");
            }
            parent = commentRepo.findById(parentId).orElseThrow(() -> new Exception("Parent comment not found"));
            if (parent.isDeleted()) throw new Exception("Cannot reply to a deleted comment");
            if (!parent.getProduct().getId().equals(productId)) {
                throw new Exception("Parent comment does not belong to this product");
            }
        }

        Comment c = Comment.builder()
                .product(product)
                .author(user)
                .text(text.trim())
                .parent(parent)
                .build();

        c = commentRepo.save(c);
        return toDto(c, false);
    }

    /**
     * List root comments paged, each with first-level replies.
     */
    public Page<CommentDto> list(Long productId, int page, int size) throws Exception {
        Product product = productRepo.findById(productId).orElseThrow(() -> new Exception("Product not found"));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> roots = commentRepo.findByProductAndParentIsNullAndDeletedFalse(product, pageable);

        List<CommentDto> mapped = roots.getContent().stream().map(root -> {
            List<Comment> replies = commentRepo.findByParentIdAndDeletedFalseOrderByCreatedAtAsc(root.getId());
            CommentDto dto = toDto(root, false);
            dto.setReplies(replies.stream().map(r -> toDto(r, false)).collect(Collectors.toList()));
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(mapped, pageable, roots.getTotalElements());
    }

    /**
     * Delete comment: owner or admin.
     */
    @Transactional
    public void delete(Long commentId, Long requesterId, boolean isAdmin) throws Exception {
        Comment c = commentRepo.findById(commentId).orElseThrow(() -> new Exception("Comment not found"));
        if (!isAdmin && !Objects.equals(c.getAuthor().getId(), requesterId)) {
            throw new Exception("You can delete only your own comment");
        }
        c.setDeleted(true);
        c.setText("[removed]");
        commentRepo.save(c);
    }

    // -------- mapping --------
    private CommentDto toDto(Comment c, boolean includeReplies) {
        return CommentDto.builder()
                .id(c.getId())
                .productId(c.getProduct().getId())
                .parentId(c.getParent() == null ? null : c.getParent().getId())
                .authorId(c.getAuthor().getId())
                .authorName(c.getAuthor().getUsername())
                .authorRole(c.getAuthor().getRole().name())
                .text(c.getText())
                .likeCount(c.getLikeCount())
                .deleted(c.isDeleted())
                .createdAt(c.getCreatedAt())
                .replies(includeReplies ? c.getReplies().stream()
                        .filter(r -> !r.isDeleted())
                        .map(r -> toDto(r, true))
                        .collect(Collectors.toList()) : null)
                .build();
    }

    @Transactional
    public String toggleLike(Long commentId, Long userId) throws Exception {
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new Exception("Comment not found"));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        // Check if the user already liked this comment
        var existing = commentLikeRepo.findByComment_IdAndUser_Id(commentId, userId);
        if (existing.isPresent()) {
            // Unlike
            commentLikeRepo.delete(existing.get());
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            commentRepo.save(comment);
            return "unliked";
        } else {
            // Like
            commentLikeRepo.save(CommentLike.builder().comment(comment).user(user).build());
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentRepo.save(comment);
            return "liked";
        }
    }
}
