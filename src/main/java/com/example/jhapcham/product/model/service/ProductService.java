package com.example.jhapcham.product.model.service;


import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.dto.ProductDto;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

import java.util.List;

@Service
public class ProductService {

    private final String uploadDir = "product-images"; // folder in local device

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product addProduct(ProductDto dto, Long sellerId) throws Exception {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new Exception("Seller not found"));

        if (!seller.getRole().equals(Role.SELLER)) {
            throw new Exception("Only sellers can add products");
        }

        String fileName = null;
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            fileName = saveImage(dto.getImage());
        }

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .category(dto.getCategory())
                .sellerId(sellerId)
                .imagePath(fileName)
                .build();

        return productRepository.save(product);
    }

    public Product updateProduct(Long productId, ProductDto dto, Long sellerId) throws Exception {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new Exception("Product not found"));

        if (!product.getSellerId().equals(sellerId)) {
            throw new Exception("You can only update your own products");
        }

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setCategory(dto.getCategory());

        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            String fileName = saveImage(dto.getImage());
            product.setImagePath(fileName);
        }

        return productRepository.save(product);
    }

    public void deleteProduct(Long productId, Long adminId) throws Exception {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new Exception("Admin not found"));

        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new Exception("Only admin can delete products");
        }

        productRepository.deleteById(productId);
    }

    public List<Product> getProductsBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId);
    }

    // Save uploaded image to local directory
    private String saveImage(MultipartFile image) throws IOException {
        Files.createDirectories(Paths.get(uploadDir));

        String originalFilename = StringUtils.cleanPath(image.getOriginalFilename());
        String fileName = System.currentTimeMillis() + "_" + originalFilename;
        Path filePath = Paths.get(uploadDir, fileName);

        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName; // store only file name in DB
    }
}