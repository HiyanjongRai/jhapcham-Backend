package com.example.jhapcham.product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import com.example.jhapcham.Cart.CartItem;
import com.example.jhapcham.Cart.CartItemRepository;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private final ProductViewRepository productViewRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private final CartItemRepository cartItemRepository;
    private final ReviewRepository reviewRepository;

    private static final String PRODUCT_IMAGE_SUBDIR = "product-images";

    // ===================== CREATE PRODUCT =====================
    @Transactional
    public ProductResponseDTO createProductForSeller(Long sellerUserId, ProductCreateRequestDTO dto) {

        User sellerUser = userRepository.findById(sellerUserId)
                .orElseThrow(() -> new RuntimeException("Seller user not found"));

        if (sellerUser.getRole() != Role.SELLER)
            throw new RuntimeException("User is not a seller");

        if (sellerUser.getStatus() != Status.ACTIVE)
            throw new RuntimeException("Seller account not active");

        SellerProfile profile = sellerProfileRepository.findByUser(sellerUser)
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        if (profile.getStatus() != Status.ACTIVE)
            throw new RuntimeException("Seller profile not active");

        LocalDate mfg = parseDate(dto.manufactureDate());
        LocalDate exp = parseDate(dto.expiryDate());

        Product product = Product.builder()
                .sellerProfile(profile)
                .name(dto.name())
                .shortDescription(dto.shortDescription())
                .description(dto.description())
                .category(dto.category())
                .brand(dto.brand())
                .specification(dto.specification())
                .storageSpec(dto.storageSpec())
                .features(dto.features())
                .colorOptions(dto.colorOptions())
                .price(dto.price())
                .stockQuantity(dto.stockQuantity())
                .warrantyMonths(dto.warrantyMonths())
                .manufactureDate(mfg)
                .expiryDate(exp)
                .onSale(false)
                .status(ProductStatus.ACTIVE)
                .build();

        Product saved = productRepository.save(product);

        // images
        if (dto.images() != null) {
            int index = 0;
            for (MultipartFile f : dto.images()) {
                if (f == null || f.isEmpty())
                    continue;

                String path = fileStorageService.save(
                        f, PRODUCT_IMAGE_SUBDIR,
                        "product_" + saved.getId() + "_" + System.currentTimeMillis() + "_" + index);

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

        // SHIPPING LOGIC
        applyShippingRules(product, dto, profile);

        return toResponse(saved);
    }

    // ===================== UPDATE PRODUCT =====================
    @Transactional
    public ProductResponseDTO updateProduct(Long productId, ProductUpdateRequestDTO dto) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        SellerProfile profile = product.getSellerProfile();

        // basic fields
        if (dto.name() != null)
            product.setName(dto.name());
        if (dto.shortDescription() != null)
            product.setShortDescription(dto.shortDescription());
        if (dto.description() != null)
            product.setDescription(dto.description());
        if (dto.category() != null)
            product.setCategory(dto.category());
        if (dto.brand() != null)
            product.setBrand(dto.brand());
        if (dto.specification() != null)
            product.setSpecification(dto.specification());
        if (dto.storageSpec() != null)
            product.setStorageSpec(dto.storageSpec());
        if (dto.features() != null)
            product.setFeatures(dto.features());
        if (dto.colorOptions() != null)
            product.setColorOptions(dto.colorOptions());
        if (dto.price() != null)
            product.setPrice(dto.price());
        if (dto.stockQuantity() != null)
            product.setStockQuantity(dto.stockQuantity());
        if (dto.warrantyMonths() != null)
            product.setWarrantyMonths(dto.warrantyMonths());
        if (dto.manufactureDate() != null)
            product.setManufactureDate(parseDate(dto.manufactureDate()));
        if (dto.expiryDate() != null)
            product.setExpiryDate(parseDate(dto.expiryDate()));

        // sale logic
        applySaleRules(product, dto);

        // image update
        if (dto.newImages() != null) {
            int idx = product.getImages().size();
            for (MultipartFile f : dto.newImages()) {
                if (f == null || f.isEmpty())
                    continue;

                String path = fileStorageService.save(
                        f, PRODUCT_IMAGE_SUBDIR,
                        "product_" + product.getId() + "_" + System.currentTimeMillis() + "_" + idx);

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

        // update shipping rules
        applyShippingRules(product, dto, profile);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    // ===================== SHIPPING RULE FUNCTION =====================
    private void applyShippingRules(Product product, Object dtoObject, SellerProfile profile) {

        boolean free;

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
            free = true;
            product.setFreeShipping(true);
            product.setInsideValleyShipping(0.0);
            product.setOutsideValleyShipping(0.0);
            product.setSellerFreeShippingMinOrder(0.0);
        } else {
            free = false;
            product.setFreeShipping(false);

            product.setInsideValleyShipping(
                    dtoInside != null ? dtoInside : profile.getInsideValleyDeliveryFee());

            product.setOutsideValleyShipping(
                    dtoOutside != null ? dtoOutside : profile.getOutsideValleyDeliveryFee());

            product.setSellerFreeShippingMinOrder(
                    profile.getFreeShippingMinOrder());
        }
    }

    // ===================== SALE LOGIC =====================
    private void applySaleRules(Product product, ProductUpdateRequestDTO dto) {

        // Nothing related to sale sent
        if (dto.onSale() == null &&
                dto.salePercentage() == null &&
                dto.discountPrice() == null) {
            return;
        }

        // Discount sent but sale switch missing
        if (dto.onSale() == null &&
                (dto.salePercentage() != null || dto.discountPrice() != null)) {
            throw new RuntimeException("Sale must be enabled to apply discount");
        }

        // Turn sale OFF
        if (!dto.onSale()) {
            product.setOnSale(false);
            product.setSalePercentage(null);
            product.setDiscountPrice(null);
            product.setSalePrice(null);
            return;
        }

        BigDecimal price = product.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid product price");
        }

        boolean hasPercentage = dto.salePercentage() != null;
        boolean hasFinalPrice = dto.discountPrice() != null;

        if (hasPercentage == hasFinalPrice) {
            throw new RuntimeException("Provide either sale percentage or final sale price");
        }

        // Reset previous sale data
        product.setOnSale(true);
        product.setSalePercentage(null);
        product.setDiscountPrice(null);
        product.setSalePrice(null);

        // CASE 1: Percentage based sale → "10% OFF"
        if (hasPercentage) {

            BigDecimal pct = dto.salePercentage();

            if (pct.compareTo(BigDecimal.ZERO) <= 0 ||
                    pct.compareTo(BigDecimal.valueOf(100)) >= 0) {
                throw new RuntimeException("Invalid sale percentage");
            }

            BigDecimal discount = price
                    .multiply(pct)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            BigDecimal salePrice = price
                    .subtract(discount)
                    .setScale(2, RoundingMode.HALF_UP);

            product.setSalePercentage(pct);
            product.setDiscountPrice(discount);
            product.setSalePrice(salePrice);
            return;
        }

        // CASE 2: Fixed final price sale → "SALE"
        BigDecimal salePrice = dto.discountPrice();

        if (salePrice.compareTo(BigDecimal.ZERO) <= 0 ||
                salePrice.compareTo(price) >= 0) {
            throw new RuntimeException("Invalid sale price");
        }

        BigDecimal discount = price
                .subtract(salePrice)
                .setScale(2, RoundingMode.HALF_UP);

        product.setSalePrice(salePrice);
        product.setDiscountPrice(discount);
        product.setSalePercentage(null); // IMPORTANT
    }

    // ===================== PRODUCT DETAIL =====================
    public ProductDetailDTO getProductDetail(Long id) {

        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        SellerProfile profile = p.getSellerProfile();
        User seller = profile.getUser();

        List<String> img = new ArrayList<>();
        for (ProductImage im : p.getImages())
            img.add(im.getImagePath());

        return ProductDetailDTO.builder()
                .productId(p.getId())
                .sellerProfileId(profile.getId())
                .sellerUserId(seller.getId())
                .name(p.getName())
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
                .stockQuantity(p.getStockQuantity())
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
                .averageRating(reviewRepository.findAverageRatingByProductId(p.getId()))
                .totalReviews(reviewRepository.countByProductId(p.getId()))
                .build();
    }

    private String buildSaleLabel(Product p) {

        if (!Boolean.TRUE.equals(p.getOnSale())) {
            return null;
        }

        if (p.getSalePercentage() != null) {
            return p.getSalePercentage()
                    .stripTrailingZeros()
                    .toPlainString() + "% OFF";
        }

        if (p.getDiscountPrice() != null) {
            return "SALE";
        }

        return "SALE";
    }

    // ===================== LIST ALL =====================
    public List<ProductResponseDTO> listAllActiveProducts() {
        return productRepository.findByStatus(ProductStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    public List<ProductResponseDTO> listProductsForSeller(Long sellerUserId) {

        User seller = userRepository.findById(sellerUserId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        SellerProfile profile = sellerProfileRepository.findByUser(seller)
                .orElseThrow(() -> new RuntimeException("Seller profile missing"));

        return productRepository.findBySellerProfile(profile)
                .stream().map(this::toResponse).toList();
    }

    // DTO response creator
    private ProductResponseDTO toResponse(Product p) {
        String saleLabel = buildSaleLabel(p);

        List<String> img = new ArrayList<>();
        for (ProductImage im : p.getImages())
            img.add(im.getImagePath());

        long views = productViewRepository.countByProduct(p);

        return ProductResponseDTO.builder()
                .id(p.getId())
                .sellerProfileId(p.getSellerProfile().getId())
                .name(p.getName())
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
                .saleLabel(saleLabel)
                .freeShipping(p.getFreeShipping())
                .insideValleyShipping(p.getInsideValleyShipping())
                .outsideValleyShipping(p.getOutsideValleyShipping())
                .sellerFreeShippingMinOrder(p.getSellerFreeShippingMinOrder())
                .stockQuantity(p.getStockQuantity())
                .warrantyMonths(p.getWarrantyMonths())
                .manufactureDate(p.getManufactureDate())
                .expiryDate(p.getExpiryDate())
                .status(p.getStatus())
                .imagePaths(img)
                .totalViews(views)

                .averageRating(reviewRepository.findAverageRatingByProductId(p.getId()))
                .totalReviews(reviewRepository.countByProductId(p.getId()))
                .sellerFullName(p.getSellerProfile().getUser().getFullName())
                .build();
    }

    private LocalDate parseDate(String v) {
        if (v == null || v.isBlank())
            return null;
        return LocalDate.parse(v);
    }

    public List<ProductViewCountProjection> getViewCountsForAllProducts() {
        return productViewRepository.countViewsByProduct();
    }

    public List<ProductResponseDTO> listActiveProductsForSeller(Long sellerUserId) {

        User seller = userRepository.findById(sellerUserId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        SellerProfile profile = sellerProfileRepository.findByUser(seller)
                .orElseThrow(() -> new RuntimeException("Seller profile missing"));

        List<Product> products = productRepository.findBySellerProfileAndStatus(profile, ProductStatus.ACTIVE);

        return products.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void hardDeleteProductWithOrderCheck(Long productId, Long sellerUserId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        SellerProfile profile = product.getSellerProfile();

        if (!profile.getUser().getId().equals(sellerUserId)) {
            throw new RuntimeException("Seller not allowed");
        }

        boolean activeOrderExists = orderRepository.existsByItemsProductAndStatusNot(product, OrderStatus.CANCELED);

        if (activeOrderExists) {
            throw new RuntimeException("Product is in active order");
        }

        List<OrderItem> orderItems = orderItemRepository.findByProduct(product);
        orderItemRepository.deleteAll(orderItems);

        List<CartItem> cartItems = cartItemRepository.findAllByProduct(product);
        cartItemRepository.deleteAll(cartItems);

        productViewRepository.deleteByProduct(product);

        productImageRepository.deleteAll(product.getImages());

        productRepository.delete(product);

    }

    public List<ProductResponseDTO> filterProducts(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String brand,
            String category) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));

        if (minPrice != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }

        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }

        if (brand != null && !brand.isBlank()) {
            predicates.add(
                    cb.like(cb.lower(root.get("brand")), "%" + brand.toLowerCase() + "%"));
        }

        if (category != null && !category.isBlank()) {
            predicates.add(
                    cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("id")));

        List<Product> products = entityManager.createQuery(cq).getResultList();

        List<ProductResponseDTO> result = new ArrayList<>();
        for (Product p : products) {
            result.add(toResponse(p));
        }

        return result;
    }

    public List<ProductResponseDTO> searchProducts(String keyword) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));

        if (keyword != null && !keyword.isBlank()) {

            String k = "%" + keyword.toLowerCase() + "%";

            predicates.add(
                    cb.or(
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

        List<Product> products = entityManager.createQuery(cq).getResultList();

        List<ProductResponseDTO> result = new ArrayList<>();
        for (Product p : products) {
            result.add(toResponse(p));
        }

        return result;
    }

    public List<ProductResponseDTO> listAllProductsForAdmin() {
        return productRepository.findAll()
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void updateProductStatus(Long productId, ProductStatus status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStatus(status);
        productRepository.save(product);
    }

}