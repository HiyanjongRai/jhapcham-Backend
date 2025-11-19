
package com.example.jhapcham.product.model.Controller;


import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.dto.ProductDto;
import com.example.jhapcham.product.model.service.ProductService;
import com.example.jhapcham.product.model.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }


    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        try {
            if (userId != null) {
                recommendationService.recordInteraction(userId, id, "VIEW", null);
            }
            
            return productService.getProductById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
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

    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            // Construct the path to the image
            Path imagePath = Path.of("product-images").resolve(filename).normalize();
            Resource resource = new UrlResource(imagePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                // Determine content type
                String contentType = Files.probeContentType(imagePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
