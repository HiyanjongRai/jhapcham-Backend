//package com.example.jhapcham.product.model.comment;
//
//import com.example.jhapcham.user.model.User;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.Optional;
//
//public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
//    boolean existsByComment_IdAndUser_Id(Long commentId, Long userId);
//    Optional<CommentLike> findByComment_IdAndUser_Id(Long commentId, Long userId);
//}
