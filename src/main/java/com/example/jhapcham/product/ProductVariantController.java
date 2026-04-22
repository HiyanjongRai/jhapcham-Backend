package com.example.jhapcham.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/products/{productId}/variants")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService variantService;

    @PostMapping
    public ResponseEntity<?> createVariant(
            @PathVariable Long productId,
            @RequestBody ProductVariantDTO dto) {
        try {
            ProductVariantDTO response = variantService.createVariant(productId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating variant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getVariants(@PathVariable Long productId) {
        try {
            List<ProductVariantDTO> variants = variantService.getProductVariants(productId);
            return ResponseEntity.ok(variants);
        } catch (Exception e) {
            log.error("Error fetching variants: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<?> getVariant(@PathVariable Long variantId) {
        try {
            ProductVariantDTO variant = variantService.getVariant(variantId);
            return ResponseEntity.ok(variant);
        } catch (Exception e) {
            log.error("Error fetching variant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{variantId}")
    public ResponseEntity<?> updateVariant(
            @PathVariable Long variantId,
            @RequestBody ProductVariantDTO dto) {
        try {
            ProductVariantDTO response = variantService.updateVariant(variantId, dto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating variant: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{variantId}/stock")
    public ResponseEntity<?> updateStock(
            @PathVariable Long variantId,
            @RequestBody Map<String, Integer> body) {
        try {
            Integer newStock = body.get("stockQuantity");
            if (newStock == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "stockQuantity is required"));
            }
            variantService.updateVariantStock(variantId, newStock);
            return ResponseEntity.ok(Map.of("message", "Stock updated successfully"));
        } catch (Exception e) {
            log.error("Error updating stock: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<?> getVariantBySku(@PathVariable String sku) {
        try {
            ProductVariantDTO variant = variantService.getVariantBySku(sku);
            return ResponseEntity.ok(variant);
        } catch (Exception e) {
            log.error("Error fetching variant by SKU: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
