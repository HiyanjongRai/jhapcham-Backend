package com.example.jhapcham.product.api;


import com.example.jhapcham.product.application.*;
import com.example.jhapcham.product.domain.*;
import com.example.jhapcham.product.dto.*;
import com.example.jhapcham.product.persistence.*;
import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;

import com.example.jhapcham.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private static final int MAX_PAGE_SIZE = 60;

    private final ProductService productService;
    private final ProductViewService productViewService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<?> listAllActiveProducts() {
        return ResponseEntity.ok(productService.listAllActiveProducts());
    }

    @GetMapping("/page")
    public ResponseEntity<?> listAllActiveProductsPaged(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "search") String keyword,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return ResponseEntity.ok(productService.listActiveProductCards(minPrice, maxPrice, brand, category, keyword, sort, pageable(page, size)));
    }

    @GetMapping("/seller/{sellerUserId}")
    public ResponseEntity<?> listActiveProductsForSeller(@PathVariable Long sellerUserId) {
        return ResponseEntity.ok(productService.listActiveProductsForSeller(sellerUserId, pageable(0, 60)).getContent());
    }

    @GetMapping("/seller/{sellerUserId}/page")
    public ResponseEntity<?> listActiveProductsForSellerPaged(
            @PathVariable Long sellerUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return ResponseEntity.ok(productService.listActiveProductsForSeller(sellerUserId, pageable(page, size)));
    }

    @GetMapping("/seller/{sellerUserId}/all")
    public ResponseEntity<?> listAllProductsForSeller(@PathVariable Long sellerUserId, Authentication authentication) {
        currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerUserId);
        return ResponseEntity.ok(productService.listProductsForSeller(sellerUserId));
    }

    @GetMapping("/my-inventory")
    public ResponseEntity<?> listMyProducts(Authentication authentication) {
        User actor = currentUserService.requireUser(authentication);
        // Seller inventory is seller-only and requires an approved seller account.
        currentUserService.requireSellerSelfOrAdmin(actor, actor.getId());
        return ResponseEntity.ok(productService.listActiveProductsForSeller(actor.getId(), pageable(0, 60)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getProductDetailBySlug(@PathVariable String slug, @RequestParam(required = false) Long userId, Authentication authentication) {
        try {
            Long resolvedUserId = null;
            if (userId != null && authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
                var actor = currentUserService.requireUser(authentication);
                if (actor.getId().equals(userId) || actor.getRole() == com.example.jhapcham.user.domain.Role.ADMIN) {
                    resolvedUserId = userId;
                }
            }
            ProductDetailDTO dto = productService.getProductDetailBySlug(slug);
            productViewService.recordView(dto.getProductId(), resolvedUserId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Failed to fetch product detail for slug {}: {}", slug, e.getMessage());
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch product detail"));
        }
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> getProductDetail(@PathVariable Long productId, @RequestParam(required = false) Long userId, Authentication authentication) {
        try {
            Long resolvedUserId = null;
            if (userId != null && authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
                var actor = currentUserService.requireUser(authentication);
                if (actor.getId().equals(userId) || actor.getRole() == com.example.jhapcham.user.domain.Role.ADMIN) {
                    resolvedUserId = userId;
                }
            }
            productViewService.recordView(productId, resolvedUserId);
            return ResponseEntity.ok(productService.getProductDetail(productId));
        } catch (Exception e) {
            log.error("Failed to fetch product detail for id {}: {}", productId, e.getMessage());
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
    public ResponseEntity<?> getRecentViewsForUser(
            @PathVariable Long userId, 
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        try {
            currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
            List<ProductView> recentViews = productViewService.getRecentViewsForUser(userId);
            
            // Get unique product IDs in the order they were viewed
            List<Long> orderedUniqueIds = recentViews.stream()
                    .map(v -> v.getProduct().getId())
                    .distinct()
                    .limit(limit) 
                    .toList();
            
            if (orderedUniqueIds.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            // Fetch products and sort them to match the original viewed order
            List<ProductResponseDTO> unorderedProducts = productService.listActiveProductsByIds(orderedUniqueIds);
            
            List<ProductResponseDTO> orderedProducts = orderedUniqueIds.stream()
                    .map(id -> unorderedProducts.stream()
                            .filter(p -> p.getId().equals(id))
                            .findFirst()
                            .orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            
            return ResponseEntity.ok(orderedProducts);
        } catch (Exception e) {
            log.error("Failed to load recent views for user {}: {}", userId, e.getMessage());
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

    @GetMapping("/filter/page")
    public ResponseEntity<?> filterProductsPaged(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "search") String keyword,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return ResponseEntity.ok(productService.listActiveProductCards(minPrice, maxPrice, brand, category, keyword, sort, pageable(page, size)));
    }

    @GetMapping("/search")
    public List<ProductResponseDTO> searchProducts(@RequestParam String keyword) {
        return productService.searchProducts(keyword);
    }

    @GetMapping("/search/page")
    public ResponseEntity<?> searchProductsPaged(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return ResponseEntity.ok(productService.searchProducts(keyword, pageable(page, size)));
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

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }
}
