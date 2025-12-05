//package com.example.jhapcham.productLike;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/likes")
//@RequiredArgsConstructor
//public class ProductLikeController {
//
//    @Autowired
//    private ProductLikeService productLikeService;
//
//    @PostMapping("/add")
//    public ResponseEntity<?> likeProduct(@RequestParam Long userId,
//                                         @RequestParam Long productId) {
//        try {
//            productLikeService.likeProduct(userId, productId);
//            return ResponseEntity.ok("Product liked successfully");
//        } catch (IllegalArgumentException | IllegalStateException e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }
//
//    @DeleteMapping("/remove")
//    public ResponseEntity<?> unlikeProduct(@RequestParam Long userId,
//                                           @RequestParam Long productId) {
//        try {
//            productLikeService.unlikeProduct(userId, productId);
//            return ResponseEntity.ok("Product unliked successfully");
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }
//
//    @GetMapping("/user")
//    public ResponseEntity<List<ProductLike>> getUserLikes(@RequestParam Long userId) {
//        try {
//            List<ProductLike> likes = productLikeService.getUserLikes(userId);
//            if (likes.isEmpty()) {
//                return ResponseEntity.ok().body(likes); // empty list
//            }
//            return ResponseEntity.ok(likes);
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.badRequest().body(null);
//        }
//    }
//
//    @GetMapping("/product-likes")
//    public ResponseEntity<List<ProductLike>> getAllLikesForProduct(@RequestParam Long productId) {
//        try {
//            List<ProductLike> likes = productLikeService.getAllLikesForProduct(productId);
//            return ResponseEntity.ok(likes);
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.badRequest().body(null);
//        }
//    }
//}
