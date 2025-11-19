package com.example.jhapcham.seller.Service;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.rating.RatingRepository;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.productLike.ProductLikeRepository;
import com.example.jhapcham.seller.dto.SellerProfileStatsDto;
import com.example.jhapcham.seller.model.SellerProfile;
import com.example.jhapcham.seller.repository.SellerProfileRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerProfileStatsService {

    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final ProductRepository productRepository;
    private final ProductLikeRepository productLikeRepository;
    private final RatingRepository ratingRepository;

    public SellerProfileStatsDto getSellerProfileStats(Long sellerUserId) {

        // User (for contact number)
        User user = userRepository.findById(sellerUserId)
                .orElseThrow(() -> new RuntimeException("Seller user not found"));

        // Seller profile (store name + address)
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerUserId)
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        // Total products
        long totalProducts = productRepository.countBySellerId(sellerUserId);

        // Total likes across all products of this seller
        long totalLikes = productLikeRepository.countBySellerId(sellerUserId);

        // Average rating across their own products
        List<Product> products = productRepository.findBySellerId(sellerUserId);

        long totalRatingCount = 0;
        double ratingSum = 0.0;

        for (Product p : products) {
            Double avgForProduct = ratingRepository.getAverageRating(p.getId());
            Integer countForProduct = ratingRepository.countRatings(p.getId());

            if (avgForProduct != null && countForProduct != null && countForProduct > 0) {
                ratingSum += avgForProduct * countForProduct;
                totalRatingCount += countForProduct;
            }
        }

        double averageRating = (totalRatingCount > 0)
                ? ratingSum / totalRatingCount
                : 0.0;

        return SellerProfileStatsDto.builder()
                .storeName(profile.getStoreName())
                .address(profile.getAddress())
                .contactNumber(user.getContactNumber())
                .totalProducts(totalProducts)
                .totalLikes(totalLikes)
                .averageRating(averageRating)
                .build();
    }
}
