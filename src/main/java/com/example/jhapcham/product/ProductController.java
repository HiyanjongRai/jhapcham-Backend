package com.example.jhapcham.product;

import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;

import com.example.jhapcham.user.model.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductViewService productViewService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<?> listAllActiveProducts() {
        return ResponseEntity.ok(productService.listAllActiveProducts());
    }

    @GetMapping("/seller/{sellerUserId}")
    public ResponseEntity<?> listActiveProductsForSeller(@PathVariable Long sellerUserId) {
        return ResponseEntity.ok(productService.listActiveProductsForSeller(sellerUserId));
    }

    @GetMapping("/seller/{sellerUserId}/all")
    public ResponseEntity<?> listAllProductsForSeller(@PathVariable Long sellerUserId, Authentication authentication) {
        currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerUserId);
        return ResponseEntity.ok(productService.listProductsForSeller(sellerUserId));
    }

    @GetMapping("/my-inventory")
    public ResponseEntity<?> listMyProducts(Authentication authentication) {
        User actor = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(productService.listProductsForSeller(actor.getId()));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getProductDetailBySlug(@PathVariable String slug, @RequestParam(required = false) Long userId, Authentication authentication) {
        try {
            Long resolvedUserId = null;
            if (userId != null && authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
                var actor = currentUserService.requireUser(authentication);
                if (actor.getId().equals(userId) || actor.getRole() == com.example.jhapcham.user.model.Role.ADMIN) {
                    resolvedUserId = userId;
                }
            }
            ProductDetailDTO dto = productService.getProductDetailBySlug(slug);
            productViewService.recordView(dto.getProductId(), resolvedUserId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch product detail"));
        }
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> getProductDetail(@PathVariable Long productId, @RequestParam(required = false) Long userId, Authentication authentication) {
        try {
            Long resolvedUserId = null;
            if (userId != null && authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
                var actor = currentUserService.requireUser(authentication);
                if (actor.getId().equals(userId) || actor.getRole() == com.example.jhapcham.user.model.Role.ADMIN) {
                    resolvedUserId = userId;
                }
            }
            productViewService.recordView(productId, resolvedUserId);
            return ResponseEntity.ok(productService.getProductDetail(productId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch product detail"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@ModelAttribute ProductCreateRequestDTO dto, Authentication authentication) {
        Long sellerUserId = currentUserService.requireUser(authentication).getId();
        return ResponseEntity.ok(productService.createProductForSeller(sellerUserId, dto));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<?> updateProduct(@PathVariable Long productId, @ModelAttribute ProductUpdateRequestDTO dto, Authentication authentication) {
        return ResponseEntity.ok(productService.updateProduct(currentUserService.requireUser(authentication).getId(), productId, dto));
    }

    @GetMapping("/views/user/{userId}/recent")
    public ResponseEntity<?> getRecentViewsForUser(@PathVariable Long userId, Authentication authentication) {
        try {
            currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
            List<ProductViewDTO> dtoList = productViewService.getRecentViewsForUser(userId).stream()
                    .map(v -> new ProductViewDTO(v.getId(), v.getProduct().getId(), v.getProduct().getName(), v.getUser() != null ? v.getUser().getId() : null, v.getUser() != null ? v.getUser().getUsername() : null, v.getViewedAt()))
                    .toList();
            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to load recent views"));
        }
    }

    @PutMapping("/{productId}/status")
    public ResponseEntity<?> updateProductStatus(@PathVariable Long productId, @RequestParam ProductStatus status, Authentication authentication) {
        try {
            productService.updateProductStatus(currentUserService.requireUser(authentication).getId(), productId, status);
            return ResponseEntity.ok("Product status updated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to update product status"));
        }
    }

    @GetMapping("/{productId}/views/count")
    public ResponseEntity<?> getTotalViewsForProduct(@PathVariable Long productId) {
        try {
            return ResponseEntity.ok(new ProductViewCountDTO(productId, productViewService.getTotalViewsForProduct(productId)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to load product view count"));
        }
    }

    @GetMapping("/filter")
    public List<ProductResponseDTO> filterProducts(@RequestParam(required = false) BigDecimal minPrice, @RequestParam(required = false) BigDecimal maxPrice, @RequestParam(required = false) String brand, @RequestParam(required = false) String category) {
        return productService.filterProducts(minPrice, maxPrice, brand, category);
    }

    @GetMapping("/search")
    public List<ProductResponseDTO> searchProducts(@RequestParam String keyword) {
        return productService.searchProducts(keyword);
    }

    @GetMapping("/views/counts-by-product")
    public ResponseEntity<?> getViewCountsForAllProducts() {
        try {
            List<ProductViewCountWithNameDTO> dtoList = productService.getViewCountsForAllProducts().stream()
                    .map(p -> new ProductViewCountWithNameDTO(p.getProductId(), p.getProductName(), p.getViewCount()))
                    .toList();
            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to load view counts"));
        }
    }

    @DeleteMapping("/{productId}/hard")
    public ResponseEntity<?> hardDeleteProduct(@PathVariable Long productId, Authentication authentication) {
        Long requesterId = currentUserService.requireUser(authentication).getId();
        productService.hardDeleteProductWithOrderCheck(productId, requesterId);
        return ResponseEntity.ok("Product deleted from database");
    }

    public record ProductViewDTO(Long viewId, Long productId, String productName, Long userId, String username, LocalDateTime viewedAt) {}
    public record ProductViewCountDTO(Long productId, long totalViews) {}
    public record ProductViewCountWithNameDTO(Long productId, String productName, long totalViews) {}
}
