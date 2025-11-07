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

    // 🟢 Get all products viewed by a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserViews(@PathVariable Long userId) {
        List<ProductViewDTO> views = productViewService.getViewsByUser(userId);
        return ResponseEntity.ok(views);
    }

    // 🟢 Optional: see top 10 most viewed products overall
    @GetMapping("/top")
    public ResponseEntity<?> getTopViewedProducts() {
        return ResponseEntity.ok(productViewService.getTopViewedProducts());
    }
}
