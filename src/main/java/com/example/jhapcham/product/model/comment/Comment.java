//package com.example.jhapcham.product.model.comment;
//
//import com.example.jhapcham.product.model.Product;
//import com.example.jhapcham.user.model.User;
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.List;
//
//@Entity
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//@Table(name = "comment")
//public class Comment {
//
////    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    // Which product this comment belongs to
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "product_id")
//    private Product product;
//
//    // Who posted this comment
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "user_id")
//    private User author;
//
//    @Column(nullable = false, length = 5000)
//    private String text;
//
//    // For replies
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "parent_id")
//    private Comment parent;
//
//    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
//    @Builder.Default
//    private List<Comment> replies = new ArrayList<>();
//
//    @Column(nullable = false)
//    private int likeCount = 0;
//
//    @CreationTimestamp
//    private Instant createdAt;
//
//    @UpdateTimestamp
//    private Instant updatedAt;
//
//    @Column(nullable = false)
//    private boolean deleted = false; // for soft delete/moderation
//}
