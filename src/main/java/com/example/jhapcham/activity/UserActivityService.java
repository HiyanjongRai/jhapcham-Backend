package com.example.jhapcham.activity;

import com.example.jhapcham.cart.CartItemRepository;
import com.example.jhapcham.order.OrderItemRepository;
import com.example.jhapcham.product.ProductViewRepository;
import com.example.jhapcham.review.ReviewRepository;
import com.example.jhapcham.wishlist.WishlistRepository;
import com.example.jhapcham.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final ProductViewRepository productViewRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void recordActivity(Long userId, Long productId, ActivityType type, String details) {
        recordIfNotExists(userId, productId, type, details);
    }

    /**
     * Sycn activities from other tables (Views, Cart, Orders, Reviews)
     * This is useful to populate the UserActivity table with existing interaction
     * data.
     */
    @Transactional
    public void syncActivitiesFromExistingData() {
        // 1. Sync Views
        productViewRepository.findAll().forEach(view -> {
            if (view.getUser() != null) {
                recordIfNotExists(view.getUser().getId(), view.getProduct().getId(), ActivityType.VIEW, null);
            }
        });

        // 2. Sync Cart Items
        cartItemRepository.findAll().forEach(item -> {
            if (item.getUser() != null) {
                recordIfNotExists(item.getUser().getId(), item.getProduct().getId(), ActivityType.ADD_TO_CART,
                        "Qty: " + item.getQuantity());
            }
        });

        // 3. Sync Orders
        orderItemRepository.findAll().forEach(item -> {
            if (item.getOrder() != null && item.getOrder().getUser() != null) {
                recordIfNotExists(item.getOrder().getUser().getId(), item.getProduct().getId(), ActivityType.ORDER,
                        "Bought " + item.getQuantity() + " item(s)");
            }
        });

        // 4. Sync Reviews
        reviewRepository.findAll().forEach(review -> {
            recordIfNotExists(review.getUser().getId(), review.getProduct().getId(), ActivityType.REVIEW,
                    review.getRating() + " stars - " + review.getComment());
        });

        // 5. Sync Wishlist
        wishlistRepository.findAll().forEach(wishlist -> {
            recordIfNotExists(wishlist.getUser().getId(), wishlist.getProduct().getId(), ActivityType.WISHLIST, null);
        });
    }

    private void recordIfNotExists(Long userId, Long productId, ActivityType type, String details) {
        List<UserActivity> existingList = userActivityRepository.findByUserIdAndProductIdAndActivityType(userId,
                productId, type);

        if (!existingList.isEmpty()) {
            // Update the first one
            UserActivity existing = existingList.get(0);
            existing.setDetails(details);
            existing.setTimestamp(java.time.LocalDateTime.now());
            userActivityRepository.save(existing);

            // Clean up duplicates if any
            if (existingList.size() > 1) {
                userActivityRepository.deleteAll(existingList.subList(1, existingList.size()));
            }
        } else {
            userActivityRepository.save(UserActivity.builder()
                    .userId(userId)
                    .productId(productId)
                    .activityType(type)
                    .details(details)
                    .build());
        }
    }

    public List<UserActivity> getUserActivities(Long userId) {
        return userActivityRepository.findByUserId(userId);
    }

    public List<UserActivityController.UserActivityResponseDTO> getUserActivitiesWithProductNames(Long userId) {
        List<UserActivity> activities = userActivityRepository.findByUserId(userId);

        if (activities.isEmpty()) {
            return List.of();
        }

        // Fetch all unique product IDs to avoid N+1 queries
        List<Long> productIds = activities.stream()
                .map(UserActivity::getProductId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> productNames = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(com.example.jhapcham.product.Product::getId,
                        com.example.jhapcham.product.Product::getName));

        return activities.stream()
                .map(a -> new UserActivityController.UserActivityResponseDTO(
                        a.getId(),
                        a.getUserId(),
                        a.getProductId(),
                        productNames.getOrDefault(a.getProductId(), "Unknown Product"),
                        a.getActivityType(),
                        a.getDetails(),
                        a.getTimestamp()))
                .collect(Collectors.toList());
    }

    /**
     * Data format for IBCF: List of interactions with weights
     */
    public List<Map<String, Object>> getInteractionsForIBCF() {
        List<UserActivity> allActivities = userActivityRepository.findAll();

        // Group by user and product, then assign a max weight or sum weights
        // For simplicity, we'll assign weights based on the activity type
        return allActivities.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getUserId() + "_" + a.getProductId()))
                .values().stream()
                .map(list -> {
                    UserActivity first = list.get(0);
                    double totalWeight = list.stream()
                            .mapToDouble(a -> getWeight(a.getActivityType()))
                            .sum();

                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("userId", first.getUserId());
                    map.put("productId", first.getProductId());
                    map.put("score", totalWeight);
                    return map;
                })
                .collect(Collectors.toList());
    }

    private double getWeight(ActivityType type) {
        return switch (type) {
            case VIEW -> 1.0;
            case WISHLIST -> 2.0;
            case ADD_TO_CART -> 3.0;
            case REVIEW -> 4.0;
            case ORDER -> 5.0;
            default -> 1.0;
        };
    }
}
