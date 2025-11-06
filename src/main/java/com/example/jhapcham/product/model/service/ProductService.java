package com.example.jhapcham.product.model.service;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.dto.ProductDto;
import com.example.jhapcham.product.model.dto.ProductResponseDTO;
import com.example.jhapcham.product.model.rating.RatingRepository;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.productLike.ProductLikeRepository;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductLikeRepository productLikeRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RatingRepository ratingRepository; // <— NEW


    private final String uploadDir = "product-images";

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // ---------- Create / Update ----------
    @Transactional
    public Product addProduct(ProductDto dto, Long sellerId) throws Exception {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new Exception("Seller not found"));
        if (seller.getRole() != Role.SELLER) throw new Exception("Only sellers can add products");

        String fileName = null;
        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            fileName = saveImage(dto.getImage());
        }

        String shortDesc = buildShortDescription(dto.getShortDescription(), dto.getDescription());

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .shortDescription(shortDesc)
                .price(dto.getPrice())
                .category(dto.getCategory())
                .sellerId(sellerId)
                .imagePath(fileName)
                .others(dto.getOthers())
                .stock(dto.getStock() != null ? dto.getStock() : 0)
                .build();

        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long productId, ProductDto dto, Long sellerId) throws Exception {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new Exception("Product not found"));

        if (!product.getSellerId().equals(sellerId)) {
            throw new Exception("You can only update your own products");
        }

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setShortDescription(buildShortDescription(dto.getShortDescription(), dto.getDescription()));
        product.setPrice(dto.getPrice());
        product.setCategory(dto.getCategory());
        product.setOthers(dto.getOthers());

        if (dto.getStock() != null) product.setStock(dto.getStock());

        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            String fileName = saveImage(dto.getImage());
            product.setImagePath(fileName);
        }

        return productRepository.save(product);
    }

    // ---------- Simple search ----------
    public List<ProductDto> searchProducts(String keyword) {
        return productRepository.searchProducts(keyword).stream()
                .map(this::toProductDto)
                .collect(Collectors.toList());
    }

    // ---------- New: filter with Specification + paging/sorting ----------
    public Page<Product> filterProducts(
            String name,
            Double minPrice, Double maxPrice,
            Double minRating, Double maxRating,
            Integer minViews, Integer maxViews,
            String category,
            Boolean visible,
            String status,     // ACTIVE, INACTIVE, DELETED, DRAFT
            Long sellerId,
            Pageable pageable
    ) {
        Specification<Product> spec = Specification.where(null);

        if (StringUtils.hasText(name)) {
            spec = spec.and((root, q, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }
        if (minPrice != null) {
            spec = spec.and((root, q, cb) -> cb.ge(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, q, cb) -> cb.le(root.get("price"), maxPrice));
        }
        if (minRating != null) {
            spec = spec.and((root, q, cb) -> cb.ge(root.get("rating"), minRating));
        }
        if (maxRating != null) {
            spec = spec.and((root, q, cb) -> cb.le(root.get("rating"), maxRating));
        }
        if (minViews != null) {
            spec = spec.and((root, q, cb) -> cb.ge(root.get("views"), minViews));
        }
        if (maxViews != null) {
            spec = spec.and((root, q, cb) -> cb.le(root.get("views"), maxViews));
        }
        if (StringUtils.hasText(category)) {
            spec = spec.and((root, q, cb) ->
                    cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
        }
        if (visible != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("visible"), visible));
        }
        if (StringUtils.hasText(status)) {
            try {
                Product.Status s = Product.Status.valueOf(status.toUpperCase());
                spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), s));
            } catch (IllegalArgumentException ignored) {
                // ignore invalid status
            }
        }
        if (sellerId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("sellerId"), sellerId));
        }

        return productRepository.findAll(spec, pageable);
    }

    // ---------- Admin / misc ----------
    @Transactional
    public void deleteProduct(Long productId, Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new Exception("Product not found"));

        if (user.getRole() == Role.ADMIN) {
            productRepository.deleteById(productId);
            return;
        }
        if (user.getRole() == Role.SELLER) {
            if (!product.getSellerId().equals(user.getId())) {
                throw new Exception("You can delete only your own products");
            }
            productRepository.deleteById(productId);
            return;
        }
        throw new Exception("You are not authorized to delete products");
    }

    public List<Product> getProductsBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId);
    }

    public ProductDto toProductDto(Product product) {
        int likeCount = productLikeRepository.countByProduct(product);
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .shortDescription(product.getShortDescription())
                .price(product.getPrice())
                .category(product.getCategory())
                .others(product.getOthers())
                .totalLikes(likeCount)
                .build();
    }

    @Transactional
    public void incrementView(Long productId) throws Exception {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new Exception("Product not found"));
        product.setViews(product.getViews() + 1);
        productRepository.save(product);
    }

    @Transactional
    public Product updateStock(Long productId, int stock) throws Exception {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new Exception("Product not found"));
        product.setStock(stock);
        return productRepository.save(product);
    }

    @Transactional
    public Product toggleVisibility(Long productId) throws Exception {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new Exception("Product not found"));
        product.setVisible(!product.isVisible());
        return productRepository.save(product);
    }

    @Transactional
    public Product updateStatus(Long productId, Product.Status status) throws Exception {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new Exception("Product not found"));
        product.setStatus(status);
        return productRepository.save(product);
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    // ProductService.java (add this method)
    public ProductResponseDTO toResponseDTO(Product p) {
        int likeCount = productLikeRepository.countByProduct(p);

        // pull a fresh snapshot
        Double avg = ratingRepository.averageForProduct(p.getId());
        long   cnt = ratingRepository.countForProduct(p.getId());

        return ProductResponseDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .shortDescription(p.getShortDescription())
                .price(p.getPrice())
                .category(p.getCategory())
                .sellerId(p.getSellerId())
                .imagePath(p.getImagePath())
                .others(p.getOthers())
                .stock(p.getStock())
                .totalLikes(likeCount)
                .totalViews(p.getViews())

                // NEW: card metrics
                .averageRating(avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0)
                .ratingCount(cnt)

                // legacy fields you already exposed
                .rating(p.getRating())
                .status(p.getStatus() == null ? null : p.getStatus().name())
                .visible(p.isVisible())
                .build();
    }



    // ---------- helpers ----------
    private String buildShortDescription(String shortDesc, String description) {
        String base = (shortDesc != null && !shortDesc.isBlank())
                ? shortDesc
                : (description != null ? description : "");
        base = base.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        int max = 160;
        if (base.length() <= max) return base;
        String cut = base.substring(0, max);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > 100) cut = cut.substring(0, lastSpace);
        return cut + "…";
    }

    private String saveImage(MultipartFile image) throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
        String original = StringUtils.getFilename(image.getOriginalFilename());
        if (original == null) throw new IOException("Invalid filename");
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot + 1).toLowerCase();
        if (!List.of("jpg", "jpeg", "png", "webp").contains(ext)) {
            throw new IOException("Unsupported image type");
        }
        String safeBase = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName = System.currentTimeMillis() + "_" + safeBase;
        Path filePath = Paths.get(uploadDir, fileName).toAbsolutePath().normalize();
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }


}
