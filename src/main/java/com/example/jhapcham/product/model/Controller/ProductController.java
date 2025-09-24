package com.example.jhapcham.product.model.Controller;

import com.example.jhapcham.product.model.Product;

import com.example.jhapcham.product.model.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/create/{sellerId}")
    public ResponseEntity<Product> createProduct(@PathVariable String sellerId, @RequestBody Product product) {
        return ResponseEntity.ok(productService.createProduct(product, sellerId));
    }

    @PutMapping("/update/{productId}/{sellerId}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long productId,
                                                 @PathVariable String sellerId,
                                                 @RequestBody Product updatedProduct) {
        return ResponseEntity.ok(productService.updateProduct(productId, updatedProduct, sellerId));
    }

    @GetMapping("/")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<Product>> getSellerProducts(@PathVariable String sellerId) {
        return ResponseEntity.ok(productService.getSellerProducts(sellerId));
    }

    @DeleteMapping("/delete/{productId}/{userId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId, @PathVariable Long userId) {
        productService.deleteProduct(productId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/view/{productId}")
    public ResponseEntity<Void> incrementView(@PathVariable Long productId) {
        productService.incrementView(productId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/sale/{productId}")
    public ResponseEntity<Void> incrementSale(@PathVariable Long productId) {
        productService.incrementSales(productId);
        return ResponseEntity.noContent().build();
    }
}
