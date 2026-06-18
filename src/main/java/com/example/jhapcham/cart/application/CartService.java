package com.example.jhapcham.cart.application;


import com.example.jhapcham.cart.application.*;
import com.example.jhapcham.cart.domain.*;
import com.example.jhapcham.cart.dto.*;
import com.example.jhapcham.cart.persistence.*;
import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.activity.domain.ActivityType;
import com.example.jhapcham.activity.application.UserActivityService;
import com.example.jhapcham.product.domain.Product;
import com.example.jhapcham.product.persistence.ProductRepository;
import com.example.jhapcham.product.domain.ProductVariant;
import com.example.jhapcham.product.persistence.ProductVariantRepository;
import com.example.jhapcham.product.application.ProductVariantService;
import com.example.jhapcham.seller.domain.SellerProfile;
import com.example.jhapcham.user.domain.User;
import com.example.jhapcham.user.persistence.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final UserActivityService userActivityService;
    private final ProductVariantService variantService;

    @Transactional
    public CartResponseDTO addToCart(Long userId, Long productId, AddToCartRequestDTO dto) {

        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new BusinessValidationException("Quantity must be greater than zero");
        }
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(Objects.requireNonNull(productId))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        boolean effectivelyHasVariants = Boolean.TRUE.equals(product.getHasVariants()) || (product.getVariants() != null && !product.getVariants().isEmpty());

        ProductVariant variant = null;
        if (effectivelyHasVariants) {
            if (dto.getVariantId() == null) {
                throw new BusinessValidationException("A variant must be selected for this product");
            }
            variant = variantRepository.findById(dto.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + dto.getVariantId()));
            
            if (!variant.getProduct().getId().equals(productId)) {
                throw new BusinessValidationException("Variant does not belong to this product");
            }
            if (!Boolean.TRUE.equals(variant.getActive())) {
                throw new BusinessValidationException("Selected variant is not available");
            }
        } else {
            if (dto.getVariantId() != null) {
                throw new BusinessValidationException("This product does not have variants. variantId must be null.");
            }
        }

        // Find existing cart item for same user + product + variant
        CartItem item = cartItemRepository.findByUserAndProductAndVariant(user, product, variant)
                .orElse(CartItem.builder()
                        .user(user)
                        .product(product)
                        .variant(variant)
                        .quantity(0)
                        .build());

        int newQty = item.getQuantity() + dto.getQuantity();
        int availableStock = (variant != null) ? variant.getStockQuantity() : product.getStockQuantity();
        
        if (availableStock < newQty) {
            String label = (variant != null) ? variant.getVariantLabel() : product.getName();
            throw new BusinessValidationException("Only " + availableStock + " items available for " + label);
        }

        item.setQuantity(newQty);
        cartItemRepository.save(item);

        userActivityService.recordActivity(userId, productId, ActivityType.ADD_TO_CART,
                "Variant: " + (variant != null ? variant.getSku() : "N/A") + " Qty: " + dto.getQuantity());

        return getCart(userId);
    }

    @Transactional
    public CartResponseDTO updateQuantity(Long userId, Long cartItemId, Integer qty) {
        if (qty == null) throw new BusinessValidationException("Quantity is required");

        CartItem item = cartItemRepository.findById(Objects.requireNonNull(cartItemId))
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getUser().getId().equals(userId)) {
            throw new AuthorizationException("You are not allowed to modify this cart item");
        }

        if (qty <= 0) {
            cartItemRepository.delete(item);
        } else {
            ProductVariant variant = item.getVariant();
            int available = (variant != null) ? variant.getStockQuantity() : item.getProduct().getStockQuantity();
            if (available < qty) {
                throw new BusinessValidationException("Only " + available + " items available in stock");
            }
            item.setQuantity(qty);
            cartItemRepository.save(item);
        }

        return getCart(userId);
    }

    @Transactional(readOnly = true)
    public CartResponseDTO getCart(Long userId) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<CartItem> items = cartItemRepository.findByUser(user);
        List<CartItemResponseDTO> list = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : items) {
            try {
                Product p = item.getProduct();
                if (p == null) {
                    cartItemRepository.delete(item);
                    continue;
                }
                // Trigger loading of lazy proxy to catch EntityNotFoundException early
                p.getName();

                ProductVariant variant = item.getVariant();
                if (variant != null) {
                    // Trigger loading of lazy proxy to catch EntityNotFoundException early
                    variant.getSku();
                }

                BigDecimal unitPrice;
                if (variant != null) {
                    unitPrice = variant.getEffectivePrice(
                        Boolean.TRUE.equals(p.getOnSale()) && p.getSalePrice() != null ? p.getSalePrice() : p.getPrice()
                    );
                } else {
                    unitPrice = Boolean.TRUE.equals(p.getOnSale()) && p.getSalePrice() != null
                            ? p.getSalePrice() : p.getPrice();
                }

                if (unitPrice == null) {
                    unitPrice = BigDecimal.ZERO;
                }

                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                subtotal = subtotal.add(lineTotal);

                String image = p.getImages() != null && !p.getImages().isEmpty()
                        ? p.getImages().get(0).getImagePath() : null;
                SellerProfile seller = p.getSellerProfile();

                // Build dynamic attributes map from variant
                Map<String, String> attrMap = new LinkedHashMap<>();
                String variantLabel = null;
                Integer stock = p.getStockQuantity();
                String sku = null;

                if (variant != null) {
                    stock = variant.getStockQuantity();
                    sku = variant.getSku();
                    variantLabel = variant.getVariantLabel();
                    variant.getAttributeValues().forEach(vav ->
                        attrMap.put(vav.getAttributeValue().getAttribute().getName(),
                                    vav.getAttributeValue().getValue())
                    );
                }

                list.add(CartItemResponseDTO.builder()
                        .cartItemId(item.getId())
                        .productId(p.getId())
                        .variantId(variant != null ? variant.getId() : null)
                        .sku(sku)
                        .name(p.getName())
                        .brand(p.getBrand())
                        .image(image)
                        .quantity(item.getQuantity())
                        .price(unitPrice)
                        .sellerId(seller != null && seller.getUser() != null ? seller.getUser().getId() : null)
                        .sellerProfileId(seller != null ? seller.getId() : null)
                        .sellerStoreName(seller != null ? seller.getStoreName() : null)
                        .freeShipping(p.getFreeShipping())
                        .insideValleyShipping(p.getInsideValleyShipping())
                        .outsideValleyShipping(p.getOutsideValleyShipping())
                        .sellerFreeShippingMinOrder(p.getSellerFreeShippingMinOrder())
                        .stockQuantity(stock)
                        .variantLabel(variantLabel)
                        .variantAttributes(attrMap)
                        .build());
            } catch (jakarta.persistence.EntityNotFoundException | org.hibernate.ObjectNotFoundException e) {
                // Self-healing: automatically remove the orphaned cart item from DB
                try {
                    cartItemRepository.delete(item);
                } catch (Exception ex) {
                    System.err.println("Failed to delete orphaned cart item: " + ex.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Skipping bad cart item ID " + item.getId() + ": " + e.getMessage());
            }
        }

        return CartResponseDTO.builder()
                .subtotal(subtotal.doubleValue())
                .items(list)
                .build();
    }
}
