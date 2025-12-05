//package com.example.jhapcham.productLike;
//
//import com.example.jhapcham.product.model.Product;
//import com.example.jhapcham.product.model.repository.ProductRepository;
//import com.example.jhapcham.user.model.User;
//import com.example.jhapcham.user.model.repository.UserRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//public class ProductLikeService {
//
//    @Autowired
//    private ProductLikeRepository productLikeRepository;
//
//    @Autowired
//    private ProductRepository productRepository;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    // Like a product
//    public void likeProduct(Long userId, Long productId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("User not found"));
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
//
//        // Prevent duplicate likes
//        if (productLikeRepository.findByUserAndProduct(user, product).isPresent()) {
//            throw new IllegalStateException("Product already liked by user");
//        }
//
//        ProductLike like = ProductLike.builder()
//                .user(user)
//                .product(product)
//                .timestamp(System.currentTimeMillis())
//                .build();
//
//        productLikeRepository.save(like);
//    }
//
//    // Unlike a product
//    public void unlikeProduct(Long userId, Long productId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("User not found"));
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
//
//        ProductLike like = productLikeRepository.findByUserAndProduct(user, product)
//                .orElseThrow(() -> new IllegalArgumentException("Like not found for this user and product"));
//
//        productLikeRepository.delete(like);
//    }
//
//    // Check if a product is liked by a user
//    public boolean isProductLikedByUser(Long userId, Long productId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("User not found"));
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
//        return productLikeRepository.findByUserAndProduct(user, product).isPresent();
//    }
//
//    // Get all likes for a product
//    public List<ProductLike> getAllLikesForProduct(Long productId) {
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
//        return productLikeRepository.findByProduct(product);
//    }
//
//    // Get all liked products of a user
//    public List<ProductLike> getUserLikes(Long userId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("User not found"));
//        return productLikeRepository.findByUser(user);
//    }
//
//    // Count number of likes for a product
//    public int countLikesForProduct(Long productId) {
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
//        return productLikeRepository.countByProduct(product);
//    }
//}