package com.example.jhapcham.product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import com.example.jhapcham.cart.CartItem;
import com.example.jhapcham.cart.CartItemRepository;
import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.Error.RoleBasedAccessException;
import com.example.jhapcham.common.CloudinaryService;
import com.example.jhapcham.common.FileStorageService;
import com.example.jhapcham.order.OrderItem;
import com.example.jhapcham.order.OrderItemRepository;
import com.example.jhapcham.order.OrderRepository;
import com.example.jhapcham.order.OrderStatus;
import com.example.jhapcham.review.ReviewRepository;
import com.example.jhapcham.seller.SellerProfile;
import com.example.jhapcham.seller.SellerProfileRepository;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.Status;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final FileStorageService fileStorageService;
    private final CloudinaryService cloudinaryService;
    private final ProductViewRepository productViewRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ReviewRepository reviewRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductVariantService variantService;

    private static final String PRODUCT_IMAGE_SUBDIR = "product-images";

    @Transactional
    public ProductResponseDTO createProductForSeller(Long sellerUserId, ProductCreateRequestDTO dto) {

        User sellerUser = userRepository.findById(sellerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        if (sellerUser.getRole() != Role.SELLER)
            throw new RoleBasedAccessException("Only sellers are allowed to perform this action.");

        if (sellerUser.getStatus() != Status.ACTIVE)
            throw new AuthorizationException("Seller account not active");

        SellerProfile profile = sellerProfileRepository.findByUser(sellerUser)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        if (profile.getStatus() != Status.ACTIVE)
            throw new AuthorizationException("Seller profile not active");

        LocalDate mfg = parseDate(dto.manufactureDate());
        LocalDate exp = parseDate(dto.expiryDate());
        validateProductPayload(
                dto.name(),
                dto.price(),
                dto.stockQuantity(),
                dto.warrantyMonths(),
                mfg,
                exp,
                dto.hasVariants(),
                dto.insideValleyShipping(),
                dto.outsideValleyShipping(),
                dto.freeShipping());

        Product product = Product.builder()
                .sellerProfile(profile)
                .name(dto.name())
                .slug(generateUniqueSlug(dto.name(), null))
                .shortDescription(dto.shortDescription())
                .description(dto.description())
                .category(dto.category())
                .brand(dto.brand())
                .specification(dto.specification())
                .storageSpec(dto.storageSpec())
                .features(dto.features())
                .colorOptions(dto.colorOptions())
                .price(dto.price())
                .stockQuantity(Boolean.TRUE.equals(dto.hasVariants()) ? 0 : (dto.stockQuantity() != null ? dto.stockQuantity() : 0))
                .warrantyMonths(dto.warrantyMonths())
                .manufactureDate(mfg)
                .expiryDate(exp)
                .onSale(false)
                .status(ProductStatus.ACTIVE)
                .hasVariants(Boolean.TRUE.equals(dto.hasVariants()))
                .build();

        if (Boolean.TRUE.equals(product.getHasVariants()) && (dto.variantsJson() == null || dto.variantsJson().isBlank() || dto.variantsJson().equals("[]"))) {
            throw new BusinessValidationException("Products with variants must include variant data.");
        }

        if (product.getCategory() != null && !product.getCategory().isBlank()) {
            ensureCategoryExists(product.getCategory());
        }

        Product saved = productRepository.save(product);

        // Upload images to Cloudinary
        if (dto.images() != null) {
            int index = 0;
            for (MultipartFile f : dto.images()) {
                if (f == null || f.isEmpty())
                    continue;

                String path = cloudinaryService.upload(f, PRODUCT_IMAGE_SUBDIR);

                ProductImage img = ProductImage.builder()
                        .product(saved)
                        .imagePath(path)
                        .mainImage(index == 0)
                        .sortOrder(index)
                        .build();

                productImageRepository.save(img);
                saved.addImage(img);
                index++;
            }
        }

        applyShippingRules(product, dto, profile);

        if (dto.variantsJson() != null) {
            variantService.createVariantsFromJson(saved, dto.variantsJson());
        }

        return toResponse(saved);
    }

    @Transactional
    public ProductResponseDTO updateProduct(Long requesterId, Long productId, ProductUpdateRequestDTO dto) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        SellerProfile profile = product.getSellerProfile();
        ensureProductOwnerOrAdmin(requesterId, product);

        LocalDate manufactureDate = dto.manufactureDate() != null ? parseDate(dto.manufactureDate()) : product.getManufactureDate();
        LocalDate expiryDate = dto.expiryDate() != null ? parseDate(dto.expiryDate()) : product.getExpiryDate();
        Boolean hasVariants = dto.hasVariants() != null ? dto.hasVariants() : product.getHasVariants();
        BigDecimal price = dto.price() != null ? dto.price() : product.getPrice();
        Integer stockQuantity = dto.stockQuantity() != null ? dto.stockQuantity() : product.getStockQuantity();
        Integer warrantyMonths = dto.warrantyMonths() != null ? dto.warrantyMonths() : product.getWarrantyMonths();
        Double insideShipping = dto.insideValleyShipping() != null ? dto.insideValleyShipping() : product.getInsideValleyShipping();
        Double outsideShipping = dto.outsideValleyShipping() != null ? dto.outsideValleyShipping() : product.getOutsideValleyShipping();
        Boolean freeShipping = dto.freeShipping() != null ? dto.freeShipping() : product.getFreeShipping();
        String name = dto.name() != null ? dto.name() : product.getName();
        validateProductPayload(name, price, stockQuantity, warrantyMonths, manufactureDate, expiryDate,
                hasVariants, insideShipping, outsideShipping, freeShipping);

        if (Boolean.TRUE.equals(hasVariants) && dto.stockQuantity() != null) {
            throw new BusinessValidationException("Variant products must be stocked through their variants only");
        }

        if (dto.name() != null) {
            product.setName(dto.name());
            product.setSlug(generateUniqueSlug(dto.name(), product.getId()));
        }
        if (dto.shortDescription() != null) product.setShortDescription(dto.shortDescription());
        if (dto.description() != null) product.setDescription(dto.description());
        if (dto.category() != null) product.setCategory(dto.category());
        if (dto.brand() != null) product.setBrand(dto.brand());
        if (dto.specification() != null) product.setSpecification(dto.specification());
        if (dto.storageSpec() != null) product.setStorageSpec(dto.storageSpec());
        if (dto.features() != null) product.setFeatures(dto.features());
        if (dto.colorOptions() != null) product.setColorOptions(dto.colorOptions());
        if (dto.price() != null) product.setPrice(dto.price());
        if (dto.stockQuantity() != null) product.setStockQuantity(dto.stockQuantity());
        if (dto.warrantyMonths() != null) product.setWarrantyMonths(dto.warrantyMonths());
        if (dto.manufactureDate() != null) product.setManufactureDate(manufactureDate);
        if (dto.expiryDate() != null) product.setExpiryDate(expiryDate);
        if (dto.hasVariants() != null) product.setHasVariants(dto.hasVariants());

        if (Boolean.TRUE.equals(product.getHasVariants()) && (dto.variantsJson() == null || dto.variantsJson().isBlank() || dto.variantsJson().equals("[]"))) {
            List<ProductVariant> existingActive = productVariantRepository.findByProductAndActive(product, true);
            if (existingActive.isEmpty()) {
                throw new BusinessValidationException("Products with variants must include variant data.");
            }
        }

        if (product.getCategory() != null && !product.getCategory().isBlank()) {
            ensureCategoryExists(product.getCategory());
        }

        applySaleRules(product, dto);

        if (dto.removeImageIds() != null) {
            for (Long imgId : dto.removeImageIds()) {
                productImageRepository.findById(imgId).ifPresent(img -> {
                    if (img.getProduct().getId().equals(product.getId())) {
                        if (img.getImagePath() != null && img.getImagePath().contains("cloudinary.com")) {
                            cloudinaryService.delete(img.getImagePath());
                        }
                        productImageRepository.delete(img);
                        product.getImages().remove(img);
                    }
                });
            }
        }

        if (dto.newImages() != null) {
            int idx = product.getImages().size();
            for (MultipartFile f : dto.newImages()) {
                if (f == null || f.isEmpty())
                    continue;

                String path = cloudinaryService.upload(f, PRODUCT_IMAGE_SUBDIR);

                ProductImage img = ProductImage.builder()
                        .product(product)
                        .imagePath(path)
                        .mainImage(false)
                        .sortOrder(idx)
                        .build();

                productImageRepository.save(img);
                product.addImage(img);
                idx++;
            }
        }

        applyShippingRules(product, dto, profile);

        Product saved = productRepository.save(product);

        if (dto.variantsJson() != null) {
            variantService.syncVariantsFromJson(saved, dto.variantsJson());
        }

        return toResponse(saved);
    }

    private void applyShippingRules(Product product, Object dtoObject, SellerProfile profile) {
        Boolean dtoFreeShipping;
        Double dtoInside;
        Double dtoOutside;

        if (dtoObject instanceof ProductCreateRequestDTO dto) {
            dtoFreeShipping = dto.freeShipping();
            dtoInside = dto.insideValleyShipping();
            dtoOutside = dto.outsideValleyShipping();
        } else {
            ProductUpdateRequestDTO dto = (ProductUpdateRequestDTO) dtoObject;
            dtoFreeShipping = dto.freeShipping();
            dtoInside = dto.insideValleyShipping();
            dtoOutside = dto.outsideValleyShipping();
        }

        if (dtoFreeShipping != null && dtoFreeShipping) {
            product.setFreeShipping(true);
            product.setInsideValleyShipping(0.0);
            product.setOutsideValleyShipping(0.0);
            product.setSellerFreeShippingMinOrder(0.0);
        } else {
            product.setFreeShipping(false);
            product.setInsideValleyShipping(dtoInside != null ? dtoInside : profile.getInsideValleyDeliveryFee());
            product.setOutsideValleyShipping(dtoOutside != null ? dtoOutside : profile.getOutsideValleyDeliveryFee());
            product.setSellerFreeShippingMinOrder(Boolean.TRUE.equals(profile.getFreeShippingEnabled()) ? profile.getFreeShippingMinOrder() : null);
        }
    }

    private void applySaleRules(Product product, ProductUpdateRequestDTO dto) {
        if (dto.onSale() == null && dto.salePercentage() == null && dto.discountPrice() == null) return;
        if (dto.onSale() == null && (dto.salePercentage() != null || dto.discountPrice() != null)) throw new BusinessValidationException("Sale must be enabled to apply discount");

        if (Boolean.FALSE.equals(dto.onSale())) {
            product.setOnSale(false);
            product.setSalePercentage(null);
            product.setDiscountPrice(null);
            product.setSalePrice(null);
            return;
        }

        BigDecimal price = product.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) throw new BusinessValidationException("Invalid product price");

        boolean hasPercentage = dto.salePercentage() != null;
        boolean hasFinalPrice = dto.discountPrice() != null;
        if (hasPercentage == hasFinalPrice) throw new BusinessValidationException("Provide either sale percentage or final sale price");

        product.setOnSale(true);
        if (hasPercentage) {
            BigDecimal pct = dto.salePercentage();
            if (pct.compareTo(BigDecimal.ZERO) <= 0 || pct.compareTo(BigDecimal.valueOf(100)) >= 0) throw new BusinessValidationException("Invalid sale percentage");
            BigDecimal discount = price.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal salePrice = price.subtract(discount).setScale(2, RoundingMode.HALF_UP);
            product.setSalePercentage(pct);
            product.setDiscountPrice(discount);
            product.setSalePrice(salePrice);
        } else {
            BigDecimal salePrice = dto.discountPrice();
            if (salePrice.compareTo(BigDecimal.ZERO) <= 0 || salePrice.compareTo(price) >= 0) throw new BusinessValidationException("Invalid sale price");
            BigDecimal discount = price.subtract(salePrice).setScale(2, RoundingMode.HALF_UP);
            product.setSalePrice(salePrice);
            product.setDiscountPrice(discount);
            product.setSalePercentage(null);
        }
    }

    public ProductDetailDTO getProductDetail(Long id) {
        Product p = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return toDetail(p);
    }

    public ProductDetailDTO getProductDetailBySlug(String slug) {
        Product p = productRepository.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return toDetail(p);
    }

    private ProductDetailDTO toDetail(Product p) {
        SellerProfile profile = p.getSellerProfile();
        User seller = profile.getUser();
        List<String> img = p.getImages().stream().map(ProductImage::getImagePath).toList();
        BigDecimal effectiveBasePrice = resolveEffectiveUnitPrice(p);
        List<ProductVariant> activeVariants = productVariantRepository.findByProductAndActive(p, true);
        Integer totalStock = activeVariants.isEmpty() ? p.getStockQuantity() : activeVariants.stream().mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0).sum();
        
        if (!activeVariants.isEmpty() && (p.getStockQuantity() == null || !p.getStockQuantity().equals(totalStock))) {
            p.setStockQuantity(totalStock);
            productRepository.save(p);
        }

        return ProductDetailDTO.builder()
                .productId(p.getId())
                .sellerProfileId(profile.getId())
                .sellerUserId(seller.getId())
                .name(p.getName())
                .slug(p.getSlug())
                .shortDescription(p.getShortDescription())
                .description(p.getDescription())
                .category(p.getCategory())
                .brand(p.getBrand())
                .specification(p.getSpecification())
                .storageSpec(p.getStorageSpec())
                .features(p.getFeatures())
                .colorOptions(p.getColorOptions())
                .price(p.getPrice())
                .saleLabel(buildSaleLabel(p))
                .discountPrice(p.getDiscountPrice())
                .salePercentage(p.getSalePercentage())
                .salePrice(p.getSalePrice())
                .freeShipping(p.getFreeShipping())
                .insideValleyShipping(p.getInsideValleyShipping())
                .outsideValleyShipping(p.getOutsideValleyShipping())
                .sellerFreeShippingMinOrder(p.getSellerFreeShippingMinOrder())
                .onSale(p.getOnSale())
                .stockQuantity(totalStock)
                .warrantyMonths(p.getWarrantyMonths())
                .manufactureDate(p.getManufactureDate())
                .expiryDate(p.getExpiryDate())
                .status(p.getStatus())
                .imagePaths(img)
                .sellerUsername(seller.getUsername())
                .sellerFullName(seller.getFullName())
                .sellerEmail(seller.getEmail())
                .sellerContactNumber(seller.getContactNumber())
                .sellerUserStatus(seller.getStatus())
                .storeName(profile.getStoreName())
                .storeAddress(profile.getAddress())
                .sellerProfileStatus(profile.getStatus())
                .logoImagePath(profile.getLogoImagePath())
                .profileImagePath(seller.getProfileImagePath())
                .averageRating(reviewRepository.findAverageRatingByProductId(p.getId()))
                .totalReviews(reviewRepository.countByProductId(p.getId()))
                .saleEndTime(p.getSaleEndTime())
                .variants(activeVariants.stream().map(v -> variantService.toDTO(v, effectiveBasePrice)).toList())
                .attributeOptions(buildAttributeOptions(p))
                .hasVariants(Boolean.TRUE.equals(p.getHasVariants()))
                .build();
    }

    private String buildSaleLabel(Product p) {
        if (!Boolean.TRUE.equals(p.getOnSale())) return null;
        if (p.getSaleLabel() != null && !p.getSaleLabel().isBlank()) return p.getSaleLabel();
        if (p.getSalePercentage() != null) return p.getSalePercentage().stripTrailingZeros().toPlainString() + "% OFF";
        return "SALE";
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> listAllActiveProducts() {
        return productRepository.findByStatus(ProductStatus.ACTIVE).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> listActiveProductsByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return List.of();
        return productRepository.findByIdInAndStatus(productIds, ProductStatus.ACTIVE).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> listProductsForSeller(Long sellerUserId) {
        User seller = userRepository.findById(sellerUserId).orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        SellerProfile profile = sellerProfileRepository.findByUser(seller).orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
        return productRepository.findBySellerProfile(profile).stream().map(this::toResponse).toList();
    }

    private ProductResponseDTO toResponse(Product p) {
        List<String> img = p.getImages().stream().map(ProductImage::getImagePath).toList();
        long views = productViewRepository.countByProduct(p);
        boolean effectivelyHasVariants = Boolean.TRUE.equals(p.getHasVariants()) || (p.getVariants() != null && !p.getVariants().isEmpty());
        List<ProductVariant> activeVariants = effectivelyHasVariants ? p.getVariants().stream().filter(v -> Boolean.TRUE.equals(v.getActive())).toList() : List.of();
        Integer displayStock = effectivelyHasVariants && !activeVariants.isEmpty() ? activeVariants.stream().mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0).sum() : (p.getStockQuantity() != null ? p.getStockQuantity() : 0);

        return ProductResponseDTO.builder()
                .id(p.getId())
                .sellerProfileId(p.getSellerProfile().getId())
                .sellerUserId(p.getSellerProfile().getUser().getId())
                .name(p.getName())
                .slug(p.getSlug())
                .shortDescription(p.getShortDescription())
                .description(p.getDescription())
                .category(p.getCategory())
                .brand(p.getBrand())
                .specification(p.getSpecification())
                .storageSpec(p.getStorageSpec())
                .features(p.getFeatures())
                .colorOptions(p.getColorOptions())
                .price(p.getPrice())
                .discountPrice(p.getDiscountPrice())
                .salePercentage(p.getSalePercentage())
                .salePrice(p.getSalePrice())
                .onSale(p.getOnSale())
                .saleLabel(buildSaleLabel(p))
                .freeShipping(p.getFreeShipping())
                .insideValleyShipping(p.getInsideValleyShipping())
                .outsideValleyShipping(p.getOutsideValleyShipping())
                .sellerFreeShippingMinOrder(p.getSellerFreeShippingMinOrder())
                .stockQuantity(displayStock)
                .warrantyMonths(p.getWarrantyMonths())
                .manufactureDate(p.getManufactureDate())
                .expiryDate(p.getExpiryDate())
                .status(p.getStatus())
                .imagePaths(img)
                .totalViews(views)
                .averageRating(reviewRepository.findAverageRatingByProductId(p.getId()))
                .totalReviews(reviewRepository.countByProductId(p.getId()))
                .sellerFullName(p.getSellerProfile().getUser().getFullName())
                .storeName(p.getSellerProfile().getStoreName())
                .logoImagePath(p.getSellerProfile().getLogoImagePath())
                .profileImagePath(p.getSellerProfile().getUser().getProfileImagePath())
                .saleEndTime(p.getSaleEndTime())
                .hasVariants(effectivelyHasVariants)
                .minPrice(calculateMinPrice(p))
                .maxPrice(calculateMaxPrice(p))
                .build();
    }

    private BigDecimal calculateMinPrice(Product p) {
        if (!Boolean.TRUE.equals(p.getHasVariants()) || p.getVariants() == null || p.getVariants().isEmpty()) return resolveEffectiveUnitPrice(p);
        return p.getVariants().stream().filter(v -> Boolean.TRUE.equals(v.getActive())).map(v -> v.getEffectivePrice(resolveEffectiveUnitPrice(p))).min(BigDecimal::compareTo).orElse(resolveEffectiveUnitPrice(p));
    }

    private BigDecimal calculateMaxPrice(Product p) {
        if (!Boolean.TRUE.equals(p.getHasVariants()) || p.getVariants() == null || p.getVariants().isEmpty()) return resolveEffectiveUnitPrice(p);
        return p.getVariants().stream().filter(v -> Boolean.TRUE.equals(v.getActive())).map(v -> v.getEffectivePrice(resolveEffectiveUnitPrice(p))).max(BigDecimal::compareTo).orElse(resolveEffectiveUnitPrice(p));
    }

    private LocalDate parseDate(String v) {
        if (v == null || v.isBlank()) return null;
        return LocalDate.parse(v);
    }

    private String generateUniqueSlug(String name, Long currentProductId) {
        String base = Normalizer.normalize(name == null ? "product" : name, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (base.isBlank()) base = "product";
        if (base.length() > 100) base = base.substring(0, 100).replaceAll("-+$", "");
        String slug = base;
        int suffix = 2;
        while (slugExists(slug, currentProductId)) {
            String ending = "-" + suffix++;
            String prefix = base.length() + ending.length() > 120 ? base.substring(0, 120 - ending.length()).replaceAll("-+$", "") : base;
            slug = prefix + ending;
        }
        return slug;
    }

    private boolean slugExists(String slug, Long currentProductId) {
        return currentProductId == null ? productRepository.existsBySlug(slug) : productRepository.existsBySlugAndIdNot(slug, currentProductId);
    }

    private java.util.Map<String, java.util.List<ProductDetailDTO.AttributeOptionDTO>> buildAttributeOptions(Product p) {
        java.util.Map<String, java.util.List<ProductDetailDTO.AttributeOptionDTO>> grouped = new java.util.LinkedHashMap<>();
        productVariantRepository.findByProductAndActive(p, true).forEach(v -> v.getAttributeValues().forEach(vav -> {
            String attrName = vav.getAttributeValue().getAttribute().getName();
            ProductDetailDTO.AttributeOptionDTO option = ProductDetailDTO.AttributeOptionDTO.builder().id(vav.getAttributeValue().getId()).value(vav.getAttributeValue().getValue()).build();
            List<ProductDetailDTO.AttributeOptionDTO> options = grouped.computeIfAbsent(attrName, k -> new java.util.ArrayList<>());
            if (options.stream().noneMatch(o -> o.getValue().equalsIgnoreCase(option.getValue()))) options.add(option);
        }));
        return grouped;
    }

    @Transactional
    public void hardDeleteProductWithOrderCheck(Long productId, Long requesterId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        ensureProductOwnerOrAdmin(requesterId, product);

        if (!orderItemRepository.findByProduct(product).isEmpty()) throw new BusinessValidationException("Cannot hard delete a product that has been ordered. Mark it inactive instead.");
        if (orderRepository.existsByItemsProductAndStatusNot(product, OrderStatus.CANCELLED)) throw new BusinessValidationException("Cannot delete product with active orders");
        
        cartItemRepository.deleteByProduct(product);
        reviewRepository.deleteByProduct(product);
        productViewRepository.deleteByProduct(product);

        // Clean up images (Local and Cloudinary)
        for (ProductImage img : product.getImages()) {
            if (img.getImagePath() != null && img.getImagePath().contains("cloudinary.com")) {
                cloudinaryService.delete(img.getImagePath());
            }
        }
        productImageRepository.deleteAll(product.getImages());

        productRepository.delete(product);
    }

    public List<ProductResponseDTO> filterProducts(BigDecimal minPrice, BigDecimal maxPrice, String brand, String category) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
        if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        if (brand != null && !brand.isBlank()) predicates.add(cb.like(cb.lower(root.get("brand")), "%" + brand.toLowerCase() + "%"));
        if (category != null && !category.isBlank()) predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("id")));
        return entityManager.createQuery(cq).getResultList().stream().map(this::toResponse).toList();
    }

    public List<ProductViewCountProjection> getViewCountsForAllProducts() {
        return productViewRepository.countViewsByProduct();
    }

    public List<ProductResponseDTO> searchProducts(String keyword) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));

        if (keyword != null && !keyword.isBlank()) {
            String k = "%" + keyword.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), k),
                    cb.like(cb.lower(root.get("brand")), k),
                    cb.like(cb.lower(root.get("shortDescription")), k),
                    cb.like(cb.lower(root.get("description")), k),
                    cb.like(cb.lower(root.get("specification")), k),
                    cb.like(cb.lower(root.get("features")), k),
                    cb.like(cb.lower(root.get("storageSpec")), k),
                    cb.like(cb.lower(root.get("colorOptions")), k)));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("id")));

        return entityManager.createQuery(cq).getResultList().stream().map(this::toResponse).toList();
    }

    public List<ProductResponseDTO> listAllProductsForAdmin() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public void updateProductStatus(Long requesterId, Long productId, ProductStatus status) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (requesterId != null) ensureProductOwnerOrAdmin(requesterId, product);
        product.setStatus(status);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> listActiveProductsForSeller(Long sellerUserId) {
        User seller = userRepository.findById(sellerUserId).orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        SellerProfile profile = sellerProfileRepository.findByUser(seller).orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
        return productRepository.findBySellerProfileAndStatus(profile, ProductStatus.ACTIVE).stream().map(this::toResponse).toList();
    }

    private void ensureProductOwnerOrAdmin(Long requesterId, Product product) {
        User actor = userRepository.findById(requesterId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (actor.getRole() == Role.ADMIN) return;
        if (actor.getRole() != Role.SELLER || !product.getSellerProfile().getUser().getId().equals(requesterId)) throw new AuthorizationException("Access denied");
    }

    public List<String> getAllCategories() { return productRepository.findDistinctCategories(); }

    private void ensureCategoryExists(String name) {
        if (name == null || name.isBlank()) return;
        String trimmed = name.trim();
        if (categoryRepository.findByName(trimmed).isEmpty()) {
            categoryRepository.save(Category.builder().name(trimmed).description("Auto-created").build());
        }
    }

    private BigDecimal resolveEffectiveUnitPrice(Product p) {
        return (Boolean.TRUE.equals(p.getOnSale()) && p.getSalePrice() != null) ? p.getSalePrice() : p.getPrice();
    }

    private void validateProductPayload(String name, BigDecimal price, Integer stockQuantity, Integer warrantyMonths, LocalDate manufactureDate, LocalDate expiryDate, Boolean hasVariants, Double insideShipping, Double outsideShipping, Boolean freeShipping) {
        if (name == null || name.isBlank()) throw new BusinessValidationException("Name required");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) throw new BusinessValidationException("Invalid price");
        if (!Boolean.TRUE.equals(hasVariants) && stockQuantity != null && stockQuantity < 0) throw new BusinessValidationException("Invalid stock");
        if (warrantyMonths != null && warrantyMonths < 0) throw new BusinessValidationException("Invalid warranty");
        if (manufactureDate != null && expiryDate != null && expiryDate.isBefore(manufactureDate)) throw new BusinessValidationException("Invalid expiry");
    }
}
