package com.example.jhapcham.product.model.comment;

import com.example.jhapcham.product.model.Product;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByProductAndParentIsNullAndDeletedFalse(Product product, Pageable pageable);
    List<Comment> findByParentIdAndDeletedFalseOrderByCreatedAtAsc(Long parentId);
}
