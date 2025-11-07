package com.example.jhapcham.product.model.ProductView;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ViewTrackingService {

    private final ProductViewRepository viewRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    /**
     * Log a product view by either a user or an anonymous guest.
     */
    @Transactional
    public void logView(Long productId, Long userId, String anonKey, String ip, String userAgent) throws Exception {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new Exception("Product not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusMinutes(5);

        boolean recentlySeen = false;

        if (userId != null) {
            recentlySeen = viewRepo.existsByUser_IdAndProduct_IdAndViewedAtBetween(userId, productId, from, now);
        } else if (anonKey != null && !anonKey.isBlank()) {
            recentlySeen = viewRepo.existsByAnonKeyAndProduct_IdAndViewedAtBetween(anonKey, productId, from, now);
        }

        // Always bump total views
        product.setViews(product.getViews() + 1);
        productRepo.save(product);

        // Record per-user or per-guest history (avoid duplicates)
        if (!recentlySeen) {
            ProductView.ProductViewBuilder builder = ProductView.builder()
                    .product(product)
                    .viewedAt(now)
                    .ip(ip)
                    .userAgent(userAgent);

            if (userId != null) {
                User u = userRepo.findById(userId).orElse(null);
                if (u != null) builder.user(u);
            } else {
                builder.anonKey(anonKey);
            }

            viewRepo.save(builder.build());
        }
    }
}
