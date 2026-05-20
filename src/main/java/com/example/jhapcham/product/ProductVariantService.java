package com.example.jhapcham.product;

import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final AttributeService attributeService;
    private final UserRepository userRepository;
    private static final int MAX_SKU_LENGTH = 150;

    /**
     * Synchronizes variants from a JSON string.
     * Logic:
     * 1. Match JSON entries with existing variants (by attribute set).
     * 2. Update existing ones, set active=true.
     * 3. Create new ones for new combinations.
     * 4. Set active=false for existing variants NOT in the JSON.
     */
    @Transactional
    public void syncVariantsFromJson(Product product, String json) {
        if (json == null) return;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> variantList = mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});

            List<Long> processedIds = new java.util.ArrayList<>();
            Set<String> submittedSkus = new HashSet<>();

            for (Map<String, Object> vMap : variantList) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rawAttrs = (Map<String, Object>) vMap.get("attributes");
                Map<String, String> attrs = normalizeAttributes(rawAttrs);
                if (attrs == null || attrs.isEmpty()) continue;

                List<Long> attrValueIds = new java.util.ArrayList<>();
                for (Map.Entry<String, String> entry : attrs.entrySet()) {
                    AttributeValue av = attributeService.findOrCreateValue(entry.getKey(), entry.getValue());
                    attrValueIds.add(av.getId());
                }

                BigDecimal price = vMap.get("price") != null && !vMap.get("price").toString().isEmpty()
                        ? new BigDecimal(vMap.get("price").toString()) : null;
                Integer stock = vMap.get("stockQuantity") != null && !vMap.get("stockQuantity").toString().isEmpty()
                        ? Integer.parseInt(vMap.get("stockQuantity").toString()) : 0;
                String sku = (String) vMap.get("sku");

                // 🛡️ Strict Validation for active variants
                if (price != null && price.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessValidationException("Variant price must be greater than zero");
                }
                if (stock != null && stock < 0) {
                    throw new BusinessValidationException("Variant stock cannot be negative");
                }

                List<ProductVariant> existing = variantRepository.findByProductAndAttributeValues(product, attrValueIds, (long) attrValueIds.size());
                ProductVariant variant;
                String normalizedSku = sku != null ? sku.trim() : null;
                validateSkuLength(normalizedSku);

                if (normalizedSku != null && !normalizedSku.isBlank() && !submittedSkus.add(normalizedSku)) {
                    throw new BusinessValidationException("Duplicate SKU in variant submission: " + normalizedSku);
                }

                if (!existing.isEmpty()) {
                    variant = existing.get(0);
                    validateSkuForSync(normalizedSku, variant.getId());
                    variant.setPrice(price);
                    variant.setStockQuantity(stock);
                    variant.setActive(true);
                    if (normalizedSku != null && !normalizedSku.isBlank()) variant.setSku(normalizedSku);
                } else {
                    validateSkuForSync(normalizedSku, null);
                    variant = ProductVariant.builder()
                            .product(product)
                            .sku(normalizedSku != null && !normalizedSku.isBlank() ? normalizedSku : generateSku(product, attrValueIds.stream().map(attributeService::getAttributeValue).toList()))
                            .price(price)
                            .stockQuantity(stock)
                            .active(true)
                            .build();
                    variantRepository.save(variant);
                    for (Long avId : attrValueIds) {
                        variant.getAttributeValues().add(VariantAttributeValue.builder()
                                .variant(variant)
                                .attributeValue(attributeService.getAttributeValue(avId))
                                .build());
                    }
                }
                variantRepository.save(variant);
                processedIds.add(variant.getId());
            }

            if (!variantList.isEmpty() && processedIds.isEmpty()) {
                throw new BusinessValidationException("At least one valid variant is required");
            }

            if (variantList.isEmpty()) {
                // If syncing empty list, inactivate all existing variants but PRESERVE product-level stock
                variantRepository.findByProduct(product).forEach(v -> {
                    v.setActive(false);
                    v.setStockQuantity(0);
                    variantRepository.save(v);
                });
                return;
            }

            // Inactivate others (variants not present in the filtered submission)
            variantRepository.findByProduct(product).forEach(v -> {
                if (!processedIds.contains(v.getId())) {
                    v.setActive(false);
                    v.setStockQuantity(0); // Safety: zero out stock for inactive
                    variantRepository.save(v);
                }
            });

            // ⚡ Aggregate Stock Calculation
            syncProductAggregateStock(product);

        } catch (BusinessValidationException bve) {
            throw bve;
        } catch (Exception e) {
            log.error("Failed to sync variants", e);
            throw new BusinessValidationException("Variant sync failed: " + e.getMessage());
        }
    }

    /**
     * Simple bulk create (legacy support or internal use).
     */
    @Transactional
    public void createVariantsFromJson(Product product, String json) {
        syncVariantsFromJson(product, json);
    }

    // ── Create ────────────────────────────────────────────────────

    @Transactional
    public ProductVariantDTO createVariant(Long actorUserId, Long productId, ProductVariantDTO.CreateRequest req) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        ensureVariantOwner(actorUserId, product);

        if (req.attributeValueIds == null || req.attributeValueIds.isEmpty()) {
            throw new BusinessValidationException("At least one attribute value is required for a variant");
        }

        // Verify none of those attribute value IDs already form an existing variant
        List<ProductVariant> existing = variantRepository.findByProductAndAttributeValues(
                product, req.attributeValueIds, (long) req.attributeValueIds.size());
        if (!existing.isEmpty()) {
            throw new BusinessValidationException("A variant with this exact attribute combination already exists");
        }

        // Fetch AttributeValue entities
        List<AttributeValue> attrValues = req.attributeValueIds.stream()
                .map(attributeService::getAttributeValue)
                .collect(Collectors.toList());

        // Auto-generate SKU if not provided
        String sku = (req.sku != null && !req.sku.isBlank())
                ? req.sku
                : generateSku(product, attrValues);
        validateVariantNumbers(req.price, req.stockQuantity);

        // Check SKU uniqueness
        variantRepository.findBySku(sku).ifPresent(v -> {
            throw new BusinessValidationException("SKU already exists: " + sku);
        });

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(sku)
                .price(req.price)
                .stockQuantity(req.stockQuantity != null ? req.stockQuantity : 0)
                .active(true)
                .build();

        variantRepository.save(variant);

        // Create the attribute mapping rows
        for (AttributeValue av : attrValues) {
            VariantAttributeValue vav = VariantAttributeValue.builder()
                    .variant(variant)
                    .attributeValue(av)
                    .build();
            variant.getAttributeValues().add(vav);
        }

        variantRepository.save(variant);
        log.info("Created variant SKU={} for productId={}", sku, productId);
        syncProductAggregateStock(product);
        return toDTO(variant, product.getPrice());
    }

    // ── Update ────────────────────────────────────────────────────

    @Transactional
    public ProductVariantDTO updateVariant(Long actorUserId, Long variantId, BigDecimal price, Integer stockQuantity,
            Boolean active) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));
        ensureVariantOwner(actorUserId, variant.getProduct());

        validateVariantNumbers(price, stockQuantity);
        if (price != null) variant.setPrice(price);
        if (stockQuantity != null) variant.setStockQuantity(stockQuantity);
        if (active != null) variant.setActive(active);

        variantRepository.save(variant);
        syncProductAggregateStock(variant.getProduct());
        log.info("Updated variant {}", variantId);
        return toDTO(variant, variant.getProduct().getPrice());
    }

    @Transactional
    public void updateStock(Long variantId, Integer newStock) {
        if (newStock == null || newStock < 0) {
            throw new BusinessValidationException("Variant stock cannot be negative");
        }
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        variant.setStockQuantity(newStock);
        variantRepository.save(variant);
        syncProductAggregateStock(variant.getProduct());
    }

    // ── Query ─────────────────────────────────────────────────────

    public List<ProductVariantDTO> getProductVariants(Long productId, boolean includeInactive) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        BigDecimal basePrice = product.getPrice();

        List<ProductVariant> variants = includeInactive
                ? variantRepository.findByProduct(product)
                : variantRepository.findByProductAndActive(product, true);

        return variants
                .stream()
                .map(v -> toDTO(v, basePrice))
                .collect(Collectors.toList());
    }

    public ProductVariantDTO getVariant(Long variantId) {
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        return toDTO(v, v.getProduct().getPrice());
    }

    /**
     * Core resolution: given a product and a set of attribute value IDs,
     * find the exactly matching active variant.
     */
    public ProductVariant resolveVariant(Long productId, List<Long> attrValueIds) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<ProductVariant> matches = variantRepository.findByProductAndAttributeValues(
                product, attrValueIds, (long) attrValueIds.size());

        if (matches.isEmpty()) {
            throw new BusinessValidationException("No variant found for selected attributes");
        }
        ProductVariant match = matches.get(0);
        if (!Boolean.TRUE.equals(match.getActive())) {
            throw new BusinessValidationException("Selected variant is not active");
        }
        return match;
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String generateSku(Product product, List<AttributeValue> attrValues) {
        String attrPart = attrValues.stream()
                .map(av -> av.getValue().replaceAll("\\s+", "-").toUpperCase())
                .collect(Collectors.joining("-"));
        String sku = "P" + product.getId() + "-" + attrPart;
        return sku.length() <= MAX_SKU_LENGTH ? sku : sku.substring(0, MAX_SKU_LENGTH);
    }

    private void validateSkuForSync(String sku, Long currentVariantId) {
        if (sku == null || sku.isBlank()) {
            return;
        }
        validateSkuLength(sku);

        variantRepository.findBySku(sku).ifPresent(existingVariant -> {
            if (currentVariantId == null || !existingVariant.getId().equals(currentVariantId)) {
                throw new BusinessValidationException("SKU already exists: " + sku);
            }
        });
    }

    private void validateSkuLength(String sku) {
        if (sku != null && sku.length() > MAX_SKU_LENGTH) {
            throw new BusinessValidationException("Variant SKU must be " + MAX_SKU_LENGTH + " characters or fewer");
        }
    }

    private Map<String, String> normalizeAttributes(Map<String, Object> rawAttrs) {
        if (rawAttrs == null || rawAttrs.isEmpty()) {
            return Map.of();
        }
        Map<String, String> attrs = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawAttrs.entrySet()) {
            String name = entry.getKey() != null ? entry.getKey().trim() : "";
            String value = entry.getValue() != null ? entry.getValue().toString().trim() : "";
            if (name.isBlank() || value.isBlank()) {
                throw new BusinessValidationException("Variant attributes must include both name and value");
            }
            attrs.put(name, value);
        }
        return attrs;
    }

    private void validateVariantNumbers(BigDecimal price, Integer stockQuantity) {
        if (price != null && price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Variant price must be greater than zero");
        }
        if (stockQuantity != null && stockQuantity < 0) {
            throw new BusinessValidationException("Variant stock cannot be negative");
        }
    }

    private void syncProductAggregateStock(Product product) {
        List<ProductVariant> activeVariants = variantRepository.findByProductAndActive(product, true);
        int totalStock = activeVariants.stream()
                .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                .sum();
        product.setStockQuantity(totalStock);
        product.setHasVariants(!activeVariants.isEmpty() || Boolean.TRUE.equals(product.getHasVariants()));
        productRepository.save(product);
    }

    public ProductVariantDTO toDTO(ProductVariant variant, BigDecimal basePrice) {
        Map<String, String> attrMap = new LinkedHashMap<>();
        List<Long> attrValueIds = variant.getAttributeValues().stream()
                .map(vav -> {
                    attrMap.put(
                        vav.getAttributeValue().getAttribute().getName(),
                        vav.getAttributeValue().getValue()
                    );
                    return vav.getAttributeValue().getId();
                })
                .collect(Collectors.toList());

        return ProductVariantDTO.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .price(variant.getEffectivePrice(basePrice))
                .stockQuantity(variant.getStockQuantity())
                .active(variant.getActive())
                .variantLabel(variant.getVariantLabel())
                .attributes(attrMap)
                .attributeValueIds(attrValueIds)
                .build();
    }

    /**
     * 🧹 Maintenance: Removes invalid or orphaned variant data.
     * Logic:
     * 1. Delete variants where price and stock are null (corrupt/placeholder).
     * 2. Delete orphaned VariantAttributeValue rows.
     * 3. Delete AttributeValues not used by any variant.
     */
    @Transactional
    public void cleanupDatabase() {
        log.info("Starting database cleanup for variants and attributes...");
        
        // 1. Delete invalid variants
        int deletedVariants = variantRepository.deleteInvalidVariants();
        log.info("Deleted {} invalid variants (null price & stock)", deletedVariants);
        
        // 2. Delete orphaned join table entries
        int deletedMappings = variantRepository.deleteOrphanedAttributeMappings();
        log.info("Deleted {} orphaned attribute mappings", deletedMappings);
        
        // 3. Delete unused attribute values
        int deletedValues = attributeValueRepository.deleteUnusedValues();
        log.info("Deleted {} unused attribute values", deletedValues);
    }

    private void ensureVariantOwner(Long actorUserId, Product product) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        if (product.getSellerProfile() == null || product.getSellerProfile().getUser() == null
                || !product.getSellerProfile().getUser().getId().equals(actorUserId)) {
            throw new AuthorizationException("You do not have permission to modify variants for this product");
        }
    }
}
