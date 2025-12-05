package com.example.jhapcham.product.model.Controller;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.ProductView.ViewTrackingService;
import com.example.jhapcham.product.model.SearchHistory.SearchHistoryService;
import com.example.jhapcham.product.model.dto.ProductDto;
import com.example.jhapcham.product.model.dto.ProductResponseDTO;
import com.example.jhapcham.product.model.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ViewTrackingService viewTrackingService;
    private final SearchHistoryService searchHistoryService;

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        List<ProductResponseDTO> out = productService.getAllProducts()
                .stream()
                .map(productService::toResponseDTO)
                .toList();
        return ResponseEntity.ok(out);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            @CookieValue(name = "anon", required = false) String anonKey,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ip,
            @RequestHeader(value = "User-Agent", required = false) String ua
    ) {
        try {
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new Exception("Product not found"));

            try {
                viewTrackingService.logView(id, userId, userId == null ? anonKey : null, ip, ua);
            } catch (Exception ignored) {
            }

            return ResponseEntity.ok(productService.toResponseDTO(product));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addProduct(@ModelAttribute ProductDto dto) {
        try {
            if (dto.getSellerId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "SellerId is required"));
            }

            Product saved = productService.addProduct(dto, dto.getSellerId());
            return ResponseEntity.ok(productService.toResponseDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @ModelAttribute ProductDto dto
    ) {
        try {
            if (dto.getSellerId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "SellerId is required"));
            }

            Product updated = productService.updateProduct(id, dto, dto.getSellerId());
            return ResponseEntity.ok(productService.toResponseDTO(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, @RequestParam Long sellerId) {
        try {
            productService.deleteProduct(id, sellerId);
            return ResponseEntity.ok(Map.of("message", "Product deleted successfully", "productId", id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/sale")
    public ResponseEntity<?> putOnSale(
            @PathVariable Long id,
            @RequestParam Long sellerId,
            @RequestParam Double discountPercent
    ) {
        try {
            Product updated = productService.putOnSale(id, sellerId, discountPercent);
            return ResponseEntity.ok(productService.toResponseDTO(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/sale")
    public ResponseEntity<?> removeSale(@PathVariable Long id, @RequestParam Long sellerId) {
        try {
            Product updated = productService.removeSale(id, sellerId);
            return ResponseEntity.ok(productService.toResponseDTO(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sale")
    public ResponseEntity<List<ProductResponseDTO>> getSaleProducts(
            @RequestParam(required = false, defaultValue = "30") Double minDiscount
    ) {
        List<ProductResponseDTO> out = productService.getSaleProducts(minDiscount);
        return ResponseEntity.ok(out);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Keyword cannot be empty");
        }

        List<Product> products = productService.searchProducts(keyword.trim());
        List<ProductResponseDTO> response = products.stream()
                .map(productService::toResponseDTO)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-products")
    public ResponseEntity<List<ProductResponseDTO>> getMyProducts(@RequestParam Long sellerId) {
        List<ProductResponseDTO> out = productService.getProductsBySeller(sellerId)
                .stream()
                .map(productService::toResponseDTO)
                .toList();
        return ResponseEntity.ok(out);
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<ProductResponseDTO>> filterProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating,
            @RequestParam(required = false) Integer minViews,
            @RequestParam(required = false) Integer maxViews,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean visible,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Boolean onSale,
            @RequestParam(required = false) LocalDate mfgStart,
            @RequestParam(required = false) LocalDate mfgEnd,
            @RequestParam(required = false) LocalDate expStart,
            @RequestParam(required = false) LocalDate expEnd,
            @RequestParam(required = false) List<String> colors,
            @RequestParam(required = false) List<String> storage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        Page<Product> result = productService.filterProducts(
                name,
                minPrice,
                maxPrice,
                minRating,
                maxRating,
                minViews,
                maxViews,
                category,
                visible,
                status,
                sellerId,
                brand,
                onSale,
                mfgStart,
                mfgEnd,
                expStart,
                expEnd,
                colors,
                storage,
                pageable
        );

        Page<ProductResponseDTO> mapped = result.map(productService::toResponseDTO);
        return ResponseEntity.ok(mapped);
    }


    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) throws IOException {
        Path base = Paths.get("H:\\Project\\Ecomm\\jhapcham\\uploads\\products").toAbsolutePath().normalize();
        Path imagePath = base.resolve(filename).normalize();

        if (!Files.exists(imagePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(imagePath.toUri());
        String contentType = Files.probeContentType(imagePath);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",", 2);
        String field = parts[0];
        Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1]))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }

}
