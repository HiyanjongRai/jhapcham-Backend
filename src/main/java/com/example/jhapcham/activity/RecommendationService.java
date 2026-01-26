package com.example.jhapcham.activity;

import com.example.jhapcham.product.ProductResponseDTO;
import com.example.jhapcham.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserActivityService userActivityService;
    private final ProductService productService;

    /**
     * Get recommendations for a user based on Item-Based Collaborative Filtering
     */
    public List<ProductResponseDTO> getRecommendations(Long userId, int limit) {
        // 1. Get all interactions
        List<Map<String, Object>> interactions = userActivityService.getInteractionsForIBCF();

        // Get all active products for fallback/predictions
        List<ProductResponseDTO> allProducts = productService.listAllActiveProducts();

        if (interactions.isEmpty()) {
            return getPopularProductsFallback(allProducts, Collections.emptySet(), limit);
        }

        // 2. Build User-Item Matrix and Item-User Matrix
        Map<Long, Map<Long, Double>> userItemMatrix = new HashMap<>();
        Map<Long, Map<Long, Double>> itemUserMatrix = new HashMap<>();
        Set<Long> allProductIdsInInteractions = new HashSet<>();

        for (Map<String, Object> interaction : interactions) {
            Long uId = (Long) interaction.get("userId");
            Long pId = (Long) interaction.get("productId");
            Double score = (Double) interaction.get("score");

            userItemMatrix.computeIfAbsent(uId, k -> new HashMap<>()).put(pId, score);
            itemUserMatrix.computeIfAbsent(pId, k -> new HashMap<>()).put(uId, score);
            allProductIdsInInteractions.add(pId);
        }

        // 3. If target user has no history, return popular products
        if (!userItemMatrix.containsKey(userId)) {
            return getPopularProductsFallback(allProducts, Collections.emptySet(), limit);
        }

        Map<Long, Double> targetUserHistory = userItemMatrix.get(userId);

        // Track items to skip (already ordered or interacted heavily)
        Set<Long> productsToSkip = targetUserHistory.entrySet().stream()
                .filter(entry -> entry.getValue() >= 3.0) // Ordered, in cart, or reviewed
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // Pre-calculate norms
        Map<Long, Double> itemNorms = new HashMap<>();
        for (Long pId : allProductIdsInInteractions) {
            double norm = 0.0;
            for (Double score : itemUserMatrix.get(pId).values()) {
                norm += Math.pow(score, 2);
            }
            itemNorms.put(pId, Math.sqrt(norm));
        }

        // 4. Calculate Predictions
        Map<Long, Double> predictions = new HashMap<>();

        for (Long candidateProductId : allProductIdsInInteractions) {
            if (productsToSkip.contains(candidateProductId))
                continue;

            double weightedSum = 0.0;
            double similaritySum = 0.0;

            for (Map.Entry<Long, Double> entry : targetUserHistory.entrySet()) {
                Long historyProductId = entry.getKey();
                Double historyScore = entry.getValue();

                double similarity = calculateCosineSimilarityOptimized(historyProductId, candidateProductId,
                        itemUserMatrix, itemNorms);

                if (similarity > 0.1) { // Threshold for meaningful similarity
                    weightedSum += similarity * historyScore;
                    similaritySum += similarity;
                }
            }

            if (similaritySum > 0) {
                predictions.put(candidateProductId, weightedSum / similaritySum);
            }
        }

        // 5. Sort predictions
        List<Long> recommendedIds = predictions.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(limit)
                .collect(Collectors.toList());

        // 6. Map back to DTOs
        Map<Long, ProductResponseDTO> productMap = allProducts.stream()
                .collect(Collectors.toMap(ProductResponseDTO::getId, p -> p));

        List<ProductResponseDTO> recommendedProducts = recommendedIds.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        // 7. Fill remaining slots with popular products
        if (recommendedProducts.size() < limit) {
            int remaining = limit - recommendedProducts.size();
            Set<Long> skipSet = new HashSet<>(productsToSkip);
            skipSet.addAll(recommendedIds);

            recommendedProducts.addAll(getPopularProductsFallback(allProducts, skipSet, remaining));
        }

        return recommendedProducts;
    }

    private List<ProductResponseDTO> getPopularProductsFallback(List<ProductResponseDTO> allProducts, Set<Long> skipIds,
            int limit) {
        return allProducts.stream()
                .filter(p -> !skipIds.contains(p.getId()))
                .filter(p -> p.getName() != null && !p.getName().toLowerCase().contains("qqqq")) // Filter junk data
                .sorted((p1, p2) -> Long.compare(
                        p2.getTotalViews() != null ? p2.getTotalViews() : 0L,
                        p1.getTotalViews() != null ? p1.getTotalViews() : 0L))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Optimized Cosine Similarity using pre-calculated norms and item-user mapping
     */
    private double calculateCosineSimilarityOptimized(Long item1, Long item2,
            Map<Long, Map<Long, Double>> itemUserMatrix, Map<Long, Double> itemNorms) {
        if (item1.equals(item2))
            return 1.0;

        Map<Long, Double> usersForItem1 = itemUserMatrix.get(item1);
        Map<Long, Double> usersForItem2 = itemUserMatrix.get(item2);

        if (usersForItem1 == null || usersForItem2 == null)
            return 0.0;

        double dotProduct = 0.0;
        // Iterate over the smaller user set
        if (usersForItem1.size() < usersForItem2.size()) {
            for (Map.Entry<Long, Double> entry : usersForItem1.entrySet()) {
                Double score2 = usersForItem2.get(entry.getKey());
                if (score2 != null) {
                    dotProduct += entry.getValue() * score2;
                }
            }
        } else {
            for (Map.Entry<Long, Double> entry : usersForItem2.entrySet()) {
                Double score1 = usersForItem1.get(entry.getKey());
                if (score1 != null) {
                    dotProduct += entry.getValue() * score1;
                }
            }
        }

        double norm1 = itemNorms.get(item1);
        double norm2 = itemNorms.get(item2);

        if (norm1 == 0 || norm2 == 0)
            return 0.0;
        return dotProduct / (norm1 * norm2);
    }
}
