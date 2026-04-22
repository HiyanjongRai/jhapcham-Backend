package com.example.jhapcham.product;

import com.example.jhapcham.Error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ProductVariantDTO createVariant(Long productId, ProductVariantDTO dto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Check if SKU already exists
        variantRepository.findBySku(dto.getSku())
                .ifPresent(v -> {
                    throw new RuntimeException("SKU already exists: " + dto.getSku());
                });

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(dto.getSku())
                .size(dto.getSize())
                .color(dto.getColor())
                .capacity(dto.getCapacity())
                .description(dto.getDescription())
                .stockQuantity(dto.getStockQuantity())
                .priceModifier(dto.getPriceModifier())
                .active(true)
                .build();

        ProductVariant saved = variantRepository.save(variant);
        log.info("Product variant created: {}", saved.getSku());
        return toDTO(saved);
    }

    @Transactional
    public ProductVariantDTO updateVariant(Long variantId, ProductVariantDTO dto) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        if (dto.getSize() != null) variant.setSize(dto.getSize());
        if (dto.getColor() != null) variant.setColor(dto.getColor());
        if (dto.getCapacity() != null) variant.setCapacity(dto.getCapacity());
        if (dto.getDescription() != null) variant.setDescription(dto.getDescription());
        if (dto.getStockQuantity() != null) variant.setStockQuantity(dto.getStockQuantity());
        if (dto.getPriceModifier() != null) variant.setPriceModifier(dto.getPriceModifier());
        if (dto.getActive() != null) variant.setActive(dto.getActive());

        ProductVariant updated = variantRepository.save(variant);
        log.info("Product variant updated: {}", variant.getSku());
        return toDTO(updated);
    }

    public List<ProductVariantDTO> getProductVariants(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        return variantRepository.findByProductAndActive(product, true)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ProductVariantDTO getVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        return toDTO(variant);
    }

    @Transactional
    public void updateVariantStock(Long variantId, Integer newStock) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        variant.setStockQuantity(newStock);
        variantRepository.save(variant);
        log.info("Variant stock updated: {} = {}", variant.getSku(), newStock);
    }

    public ProductVariantDTO getVariantBySku(String sku) {
        ProductVariant variant = variantRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Variant with SKU not found: " + sku));
        return toDTO(variant);
    }

    private ProductVariantDTO toDTO(ProductVariant variant) {
        return ProductVariantDTO.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .size(variant.getSize())
                .color(variant.getColor())
                .capacity(variant.getCapacity())
                .description(variant.getDescription())
                .stockQuantity(variant.getStockQuantity())
                .priceModifier(variant.getPriceModifier())
                .active(variant.getActive())
                .variantName(variant.getVariantName())
                .build();
    }
}
