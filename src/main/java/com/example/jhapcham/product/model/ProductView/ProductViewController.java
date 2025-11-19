package com.example.jhapcham.product.model.ProductView;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/views")
@RequiredArgsConstructor
public class ProductViewController {

    private final ProductViewService productViewService;
    private final ViewTrackingService viewTrackingService;       // FIXED
    private final ProductViewRepository productViewRepository;   // FIXED

    // 🟢 Log a product view
    @PostMapping("/log")
    public ResponseEntity<?> logView(
            @RequestParam Long productId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String anonKey,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ip,
            @RequestHeader(value = "User-Agent", required = false) String userAgent
    ) {
        try {
            String realIp = (ip != null) ? ip : "unknown";

            viewTrackingService.logView(
                    productId,
                    userId,
                    anonKey,
                    realIp,
                    userAgent
            );

            return ResponseEntity.ok("Logged");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🟢 Get all products viewed by a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserViews(@PathVariable Long userId) {
        List<ProductViewDTO> views = productViewService.getViewsByUser(userId);
        return ResponseEntity.ok(views);
    }

    // 🟢 Get top viewed products by this user
    @GetMapping("/user/{userId}/top")
    public ResponseEntity<?> getTopViewedByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(productViewRepository.findTopByUser(userId));
    }

    // 🟢 Top 10 products globally
    @GetMapping("/top")
    public ResponseEntity<?> getTopViewedProducts() {
        return ResponseEntity.ok(productViewService.getTopViewedProducts());
    }
}
