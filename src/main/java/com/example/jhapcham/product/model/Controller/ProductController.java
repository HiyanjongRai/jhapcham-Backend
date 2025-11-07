package com.example.jhapcham.product.model.Controller;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.ProductView.ViewTrackingService;
import com.example.jhapcham.product.model.SearchHistory.SearchHistoryService;
import com.example.jhapcham.product.model.dto.ProductDto;
import com.example.jhapcham.product.model.dto.ProductResponseDTO;
import com.example.jhapcham.product.model.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ViewTrackingService viewTrackingService;


    @Autowired
    private SearchHistoryService searchHistoryService;

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
            Product p = productService.getProductById(id)
                    .orElseThrow(() -> new Exception("Product not found"));

            try {
                viewTrackingService.logView(id, userId, (userId == null ? anonKey : null), ip, ua);
            } catch (Exception ignored) {}

            return ResponseEntity.ok(productService.toResponseDTO(p));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    // -------- CREATE --------
    @PostMapping("/add")
    public ResponseEntity<?> addProduct(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam(required = false) String shortDescription,
            @RequestParam Double price,
            @RequestParam String category,
            @RequestParam Long sellerId,
            @RequestParam(required = false) Integer stock,
            @RequestParam(required = false) String others,
            @RequestParam(required = false) MultipartFile image
    ) {
        try {
            ProductDto dto = ProductDto.builder()
                    .name(name)
                    .description(description)
                    .shortDescription(shortDescription)
                    .price(price)
                    .category(category)
                    .stock(stock)
                    .others(others)
                    .image(image)
                    .build();

            Product saved = productService.addProduct(dto, sellerId);
            return ResponseEntity.ok(productService.toResponseDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // -------- UPDATE --------
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam(required = false) String shortDescription,
            @RequestParam Double price,
            @RequestParam String category,
            @RequestParam Long sellerId,
            @RequestParam(required = false) Integer stock,
            @RequestParam(required = false) String others,
            @RequestParam(required = false) MultipartFile image
    ) {
        try {
            ProductDto dto = ProductDto.builder()
                    .name(name)
                    .description(description)
                    .shortDescription(shortDescription)
                    .price(price)
                    .category(category)
                    .stock(stock)
                    .others(others)
                    .image(image)
                    .build();
            Product saved = productService.updateProduct(id, dto, sellerId);
            return ResponseEntity.ok(productService.toResponseDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // -------- DELETE --------
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, @RequestParam Long userId) {
        try {
            productService.deleteProduct(id, userId);
            return ResponseEntity.ok(Map.of("message", "Product deleted successfully", "productId", id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // -------- VIEWS / STOCK / VISIBILITY / STATUS --------
    @PostMapping("/{id}/view")
    public ResponseEntity<?> incrementView(@PathVariable Long id) {
        try {
            productService.incrementView(id);
            return ResponseEntity.ok(Map.of("message", "View count incremented"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> updateStock(@PathVariable Long id, @RequestParam int stock) {
        try {
            Product updated = productService.updateStock(id, stock);
            return ResponseEntity.ok(productService.toResponseDTO(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<?> toggleVisibility(@PathVariable Long id) {
        try {
            Product updated = productService.toggleVisibility(id);
            return ResponseEntity.ok(productService.toResponseDTO(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            Product.Status s = Product.Status.valueOf(status.toUpperCase());
            Product updated = productService.updateStatus(id, s);
            return ResponseEntity.ok(productService.toResponseDTO(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // -------- SEARCH (integrated + search history logging) --------
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDTO>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(required = false) Long userId
    ) {
        if (userId != null) {
            searchHistoryService.logSearch(userId, keyword);
        }

        List<ProductResponseDTO> out = productService.searchProducts(keyword)
                .stream()
                .map(dto -> productService.getProductById(dto.getId()).orElse(null))
                .filter(Objects::nonNull)
                .map(productService::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(out);
    }

    // -------- SELLER PRODUCTS --------
    @GetMapping("/my-products")
    public ResponseEntity<List<ProductResponseDTO>> getMyProducts(@RequestParam Long sellerId) {
        List<ProductResponseDTO> out = productService.getProductsBySeller(sellerId)
                .stream()
                .map(productService::toResponseDTO)
                .toList();
        return ResponseEntity.ok(out);
    }

    // -------- FILTER (paged DTOs) --------
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<Product> result = productService.filterProducts(
                name, minPrice, maxPrice, minRating, maxRating,
                minViews, maxViews, category, visible, status, sellerId, pageable
        );
        Page<ProductResponseDTO> mapped = result.map(productService::toResponseDTO);
        return ResponseEntity.ok(mapped);
    }

    // -------- IMAGES --------
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Path base = Path.of("product-images").toAbsolutePath().normalize();
            Path imagePath = base.resolve(filename).normalize();
            if (!imagePath.startsWith(base)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

            Resource resource = new UrlResource(imagePath.toUri());
            if (!resource.exists() || !resource.isReadable()) return ResponseEntity.notFound().build();

            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",", 2);
        String field = parts[0];
        Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1]))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }
}
