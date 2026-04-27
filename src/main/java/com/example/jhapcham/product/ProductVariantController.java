package com.example.jhapcham.product;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService variantService;

    /** List all active variants for a product */
    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<ProductVariantDTO>> getVariants(@PathVariable Long productId) {
        return ResponseEntity.ok(variantService.getProductVariants(productId));
    }

    /** Get a single variant */
    @GetMapping("/variants/{variantId}")
    public ResponseEntity<ProductVariantDTO> getVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(variantService.getVariant(variantId));
    }

    /**
     * Create a new variant.
     * Body example:
     * {
     *   "sku": "optional-custom-sku",
     *   "price": 1299.00,
     *   "stockQuantity": 50,
     *   "attributeValueIds": [1, 5, 9]
     * }
     */
    @PostMapping("/{productId}/variants")
    public ResponseEntity<ProductVariantDTO> createVariant(
            @PathVariable Long productId,
            @RequestBody ProductVariantDTO.CreateRequest req) {
        return ResponseEntity.ok(variantService.createVariant(productId, req));
    }

    /**
     * Update stock, price or active flag of a variant.
     */
    @PatchMapping("/variants/{variantId}")
    public ResponseEntity<ProductVariantDTO> updateVariant(
            @PathVariable Long variantId,
            @RequestBody Map<String, Object> body) {

        BigDecimal price = body.containsKey("price")
                ? new BigDecimal(body.get("price").toString()) : null;
        Integer stock = body.containsKey("stockQuantity")
                ? Integer.parseInt(body.get("stockQuantity").toString()) : null;
        Boolean active = body.containsKey("active")
                ? Boolean.parseBoolean(body.get("active").toString()) : null;

        return ResponseEntity.ok(variantService.updateVariant(variantId, price, stock, active));
    }

    /**
     * Resolve variant from a set of attribute value IDs.
     * Body: { "productId": 5, "attributeValueIds": [1, 5] }
     */
    @PostMapping("/variants/resolve")
    public ResponseEntity<ProductVariantDTO> resolveVariant(@RequestBody Map<String, Object> body) {
        Long productId = Long.parseLong(body.get("productId").toString());
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("attributeValueIds");
        List<Long> attrValueIds = rawIds.stream().map(Long::valueOf).toList();

        ProductVariant v = variantService.resolveVariant(productId, attrValueIds);
        return ResponseEntity.ok(variantService.toDTO(v, v.getProduct().getPrice()));
    }
}
