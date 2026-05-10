package com.example.jhapcham.product;

import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;

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

    // ===================== CUSTOMER LIST =====================
    @GetMapping
    public ResponseEntity<?> listAllActiveProducts() {
        try {
            List<ProductResponseDTO> list = productService.listAllActiveProducts();
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Failed to fetch products"));
        }
    }

    // ===================== SELLER ACTIVE LIST =====================
    @GetMapping("/seller/{sellerUserId}")
    public ResponseEntity<?> listActiveProductsForSeller(@PathVariable Long sellerUserId) {
        try {
            List<ProductResponseDTO> list = productService.listActiveProductsForSeller(sellerUserId);
            return ResponseEntity.ok(list);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Failed to fetch products"));
        }
    }

    // ===================== SELLER FULL LIST =====================
    @GetMapping("/seller/{sellerUserId}/all")
    public ResponseEntity<?> listAllProductsForSeller(@PathVariable Long sellerUserId, Authentication authentication) {
        try {
            currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerUserId);
            List<ProductResponseDTO> list = productService.listProductsForSeller(sellerUserId);
            return ResponseEntity.ok(list);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Failed to fetch products"));
        }
    }

    // ===================== PRODUCT DETAIL + RECORD VIEW =====================
    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getProductDetailBySlug(
            @PathVariable String slug,
            @RequestParam(required = false) Long userId,
            Authentication authentication) {
        try {
            Long resolvedUserId = null;
            if (userId != null && authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getName())) {
                var actor = currentUserService.requireUser(authentication);
                if (actor.getId().equals(userId) || actor.getRole() == com.example.jhapcham.user.model.Role.ADMIN) {
                    resolvedUserId = userId;
                }
            }

            ProductDetailDTO dto = productService.getProductDetailBySlug(slug);
            productViewService.recordView(dto.getProductId(), resolvedUserId);
            return ResponseEntity.ok(dto);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Failed to fetch product detail"));
        }
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> getProductDetail(
            @PathVariable Long productId,
            @RequestParam(required = false) Long userId,
            Authentication authentication) {
        try {
            Long resolvedUserId = null;
            if (userId != null && authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getName())) {
                var actor = currentUserService.requireUser(authentication);
                if (actor.getId().equals(userId) || actor.getRole() == com.example.jhapcham.user.model.Role.ADMIN) {
                    resolvedUserId = userId;
                }
            }
            productViewService.recordView(productId, resolvedUserId);

            ProductDetailDTO dto = productService.getProductDetail(productId);
            return ResponseEntity.ok(dto);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Failed to fetch product detail"));
        }
    }

    // ===================== CREATE PRODUCT =====================
    @PostMapping("/seller/{sellerUserId}")
    public ResponseEntity<?> createProduct(
            @PathVariable Long sellerUserId,
            @ModelAttribute ProductCreateRequestDTO dto,
            Authentication authentication) {
        try {
            currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerUserId);
            ProductResponseDTO response = productService.createProductForSeller(sellerUserId, dto);
            return ResponseEntity.ok(response);
        } catch (MultipartException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid file upload"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Failed to create product"));
        }
    }

    // ===================== UPDATE PRODUCT WITH SALE % =====================
    @PutMapping("/{productId}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long productId,
            @ModelAttribute ProductUpdateRequestDTO dto,
            Authentication authentication) {
        try {
            ProductResponseDTO response = productService.updateProduct(currentUserService.requireUser(authentication).getId(),
                    productId, dto);
            return ResponseEntity.ok(response);
        } catch (MultipartException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid file upload"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Failed to update product"));
        }
    }

    // ===================== RECENT VIEWS =====================
    @GetMapping("/views/user/{userId}/recent")
    public ResponseEntity<?> getRecentViewsForUser(@PathVariable Long userId, Authentication authentication) {
        try {
            currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
            List<ProductView> views = productViewService.getRecentViewsForUser(userId);

            List<ProductViewDTO> dtoList = views.stream()
                    .map(v -> new ProductViewDTO(
                            v.getId(),
                            v.getProduct().getId(),
                            v.getProduct().getName(),
                            v.getUser() != null ? v.getUser().getId() : null,
                            v.getUser() != null ? v.getUser().getUsername() : null,
                            v.getViewedAt()))
                    .toList();

            return ResponseEntity.ok(dtoList);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Failed to load recent views"));
        }
    }

    // ===================== UPDATE PRODUCT STATUS (VISIBILITY) =====================
    @PutMapping("/{productId}/status")
    public ResponseEntity<?> updateProductStatus(
            @PathVariable Long productId,
            @RequestParam ProductStatus status,
            Authentication authentication) {
        try {
            productService.updateProductStatus(currentUserService.requireUser(authentication).getId(), productId, status);
            return ResponseEntity.ok("Product status updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Failed to update product status"));
        }
    }

    // ===================== SINGLE PRODUCT VIEW COUNT =====================
    @GetMapping("/{productId}/views/count")
    public ResponseEntity<?> getTotalViewsForProduct(@PathVariable Long productId) {
        try {
            long count = productViewService.getTotalViewsForProduct(productId);
            return ResponseEntity.ok(new ProductViewCountDTO(productId, count));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Failed to load product view count"));
        }
    }

    @GetMapping("/filter")
    public List<ProductResponseDTO> filterProducts(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category) {
        return productService.filterProducts(minPrice, maxPrice, brand, category);
    }

    @GetMapping("/search")
    public List<ProductResponseDTO> searchProducts(
            @RequestParam String keyword) {
        return productService.searchProducts(keyword);
    }

    // ===================== ALL PRODUCTS VIEW COUNT =====================
    @GetMapping("/views/counts-by-product")
    public ResponseEntity<?> getViewCountsForAllProducts() {
        try {
            List<ProductViewCountProjection> projections = productService.getViewCountsForAllProducts();

            List<ProductViewCountWithNameDTO> dtoList = projections.stream()
                    .map(p -> new ProductViewCountWithNameDTO(
                            p.getProductId(),
                            p.getProductName(),
                            p.getViewCount()))
                    .toList();

            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Failed to load view counts"));
        }
    }

    // RECORD DTOs --------------------------------------------------
    public static record ProductViewDTO(
            Long viewId,
            Long productId,
            String productName,
            Long userId,
            String username,
            LocalDateTime viewedAt) {
    }

    public static record ProductViewCountDTO(
            Long productId,
            long totalViews) {
    }

    public static record ProductViewCountWithNameDTO(
            Long productId,
            String productName,
            long totalViews) {
    }

    @DeleteMapping("/{productId}/seller/{sellerUserId}/hard")
    public ResponseEntity<?> hardDeleteProduct(
            @PathVariable Long productId,
            @PathVariable Long sellerUserId,
            Authentication authentication) {
        try {
            currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerUserId);
            productService.hardDeleteProductWithOrderCheck(productId, sellerUserId);
            return ResponseEntity.ok("Product deleted from database");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Failed to delete product"));
        }
    }

}
