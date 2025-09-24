package com.example.jhapcham.product.model.service;

import com.example.jhapcham.product.model.Product;

import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.Status;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    private User getSellerOrThrow(String sellerId) {
        Long sellerLongId;
        try {
            sellerLongId = Long.parseLong(sellerId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid seller id");
        }
        User seller = userRepository.findById(sellerLongId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));
        if (seller.getRole() != Role.SELLER) {
            throw new IllegalStateException("Only SELLER can create/update products");
        }
        if (seller.getStatus() != Status.ACTIVE) {
            throw new IllegalStateException("Seller is not approved");
        }
        return seller;
    }

    @Transactional
    public Product createProduct(Product product, String sellerId) {
        User seller = getSellerOrThrow(sellerId);
        product.setSellerId(seller.getId().toString());
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long productId, Product updatedProduct, String sellerId) {
        User seller = getSellerOrThrow(sellerId);

        Product existing = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (!seller.getId().toString().equals(existing.getSellerId())) {
            throw new IllegalStateException("Seller can update only their own product");
        }

        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        existing.setStock(updatedProduct.getStock());
        existing.setCategory(updatedProduct.getCategory());
        existing.setBrand(updatedProduct.getBrand());
        existing.setSku(updatedProduct.getSku());
        existing.setImageUrl(updatedProduct.getImageUrl());
        existing.setDiscountPrice(updatedProduct.getDiscountPrice());
        existing.setIsActive(updatedProduct.getIsActive() != null ? updatedProduct.getIsActive() : existing.getIsActive());
        existing.setSearchRank(updatedProduct.getSearchRank() != null ? updatedProduct.getSearchRank() : existing.getSearchRank());
        existing.setTags(updatedProduct.getTags());

        return productRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll().stream()
                .filter(Product::getIsActive)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Product> getSellerProducts(String sellerId) {
        return productRepository.findBySellerId(sellerId);
    }

    @Transactional
    public void deleteProduct(Long productId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (user.getRole() == Role.ADMIN) {
            productRepository.delete(product);
            return;
        }

        if (user.getRole() == Role.SELLER && user.getId().toString().equals(product.getSellerId())) {
            productRepository.delete(product);
            return;
        }

        throw new IllegalStateException("Not authorized to delete this product");
    }

    @Transactional
    public void incrementView(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.setViewsCount(product.getViewsCount() + 1);
        productRepository.save(product);
    }

    @Transactional
    public void incrementSales(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.setSalesCount(product.getSalesCount() + 1);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getAllSortedBy(String sortBy, boolean desc) {
        Sort sort = desc ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return productRepository.findAll(sort).stream()
                .filter(Product::getIsActive)
                .toList();
    }
}
