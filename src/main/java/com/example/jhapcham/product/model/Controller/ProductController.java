package com.example.jhapcham.product.model.Controller;


import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.dto.ProductDto;
import com.example.jhapcham.product.model.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @PostMapping("/add")
    public ResponseEntity<?> addProduct(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam Double price,
            @RequestParam String category,
            @RequestParam Long sellerId,
            @RequestParam(required = false) MultipartFile image
    ) {
        try {
            ProductDto dto = ProductDto.builder()
                    .name(name)
                    .description(description)
                    .price(price)
                    .category(category)
                    .image(image)
                    .build();
            Product product = productService.addProduct(dto, sellerId);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam Double price,
            @RequestParam String category,
            @RequestParam Long sellerId,
            @RequestParam(required = false) MultipartFile image
    ) {
        try {
            ProductDto dto = ProductDto.builder()
                    .name(name)
                    .description(description)
                    .price(price)
                    .category(category)
                    .image(image)
                    .build();
            Product product = productService.updateProduct(id, dto, sellerId);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, @RequestParam Long adminId) {
        try {
            productService.deleteProduct(id, adminId);
            return ResponseEntity.ok("Product deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my-products")
    public ResponseEntity<List<Product>> getMyProducts(@RequestParam Long sellerId) {
        return ResponseEntity.ok(productService.getProductsBySeller(sellerId));
    }
}
