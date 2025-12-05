//package com.example.jhapcham.seller.Controller;
//
//import com.example.jhapcham.product.model.Product;
//import com.example.jhapcham.product.model.dto.ProductResponseDTO;
//import com.example.jhapcham.product.model.repository.ProductRepository;
//import com.example.jhapcham.product.model.service.ProductService;
//import com.example.jhapcham.product.model.rating.RatingRepository;
//import com.example.jhapcham.review.ReviewRepository;
//import com.example.jhapcham.seller.dto.SellerFullDetailsDto;
//import com.example.jhapcham.seller.model.SellerProfile;
//import com.example.jhapcham.seller.repository.SellerProfileRepository;
//import com.example.jhapcham.user.model.User;
//import com.example.jhapcham.user.model.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class SellerFullDetailsService {
//
//    private final UserRepository userRepository;
//    private final SellerProfileRepository sellerProfileRepository;
//    private final ProductRepository productRepository;
//    private final ReviewRepository reviewRepository;
//    private final ProductService productService;
//
//    public SellerFullDetailsDto getSellerFullDetails(Long sellerUserId) {
//
//        User user = userRepository.findById(sellerUserId)
//                .orElseThrow(() -> new RuntimeException("Seller user not found"));
//
//        SellerProfile profile = sellerProfileRepository.findByUserId(sellerUserId)
//                .orElseThrow(() -> new RuntimeException("Seller profile not found"));
//
//        List<Product> sellerProducts = productRepository.findBySellerId(sellerUserId);
//
//        List<ProductResponseDTO> productDTOs = sellerProducts.stream()
//                .map(productService::toResponseDTO)
//                .toList();
//
//        double avgRating = getSellerAverageRating(sellerUserId);
//        long ratingCount = getSellerRatingCount(sellerUserId);
//
//        return SellerFullDetailsDto.builder()
//                .userId(user.getId())
//                .username(user.getUsername())
//                .fullName(user.getFullName())
//                .email(user.getEmail())
//                .contactNumber(user.getContactNumber())
//                .storeName(profile.getStoreName())
//                .storeAddress(profile.getAddress())
//                .averageRating(avgRating)
//                .ratingCount(ratingCount)
//                .products(productDTOs)
//                .build();
//    }
//
//    /* Seller average rating = weighted average of all product ratings */
//    public double getSellerAverageRating(Long sellerId) {
//
//        List<Product> products = productRepository.findBySellerId(sellerId);
//
//        double sum = 0.0;
//        long totalReviews = 0L;
//
//        for (Product p : products) {
//
//            Double avg = reviewRepository.getAverageRating(p.getId());
//            Long count = reviewRepository.getReviewCount(p.getId());
//
//            if (avg != null && count != null && count > 0) {
//                sum += avg * count;
//                totalReviews += count;
//            }
//        }
//
//        return totalReviews > 0 ? sum / totalReviews : 0.0;
//    }
//
//    /* Total number of all reviews of the seller */
//    public long getSellerRatingCount(Long sellerId) {
//
//        List<Product> products = productRepository.findBySellerId(sellerId);
//
//        long total = 0L;
//
//        for (Product p : products) {
//            Long count = reviewRepository.getReviewCount(p.getId());
//            total += (count != null ? count : 0L);
//        }
//
//        return total;
//    }
//
//}
