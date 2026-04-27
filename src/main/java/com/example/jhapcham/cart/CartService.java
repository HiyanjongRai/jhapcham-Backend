package com.example.jhapcham.cart;

import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.activity.ActivityType;
import com.example.jhapcham.activity.UserActivityService;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.product.ProductVariant;
import com.example.jhapcham.product.ProductVariantRepository;
import com.example.jhapcham.product.ProductVariantService;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import jakarta.transaction.Transactional;
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
        if (dto.getVariantId() == null) {
            throw new BusinessValidationException("A variant must be selected before adding to cart");
        }

        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(Objects.requireNonNull(productId))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductVariant variant = variantRepository.findById(dto.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + dto.getVariantId()));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessValidationException("Variant does not belong to this product");
        }

        if (!Boolean.TRUE.equals(variant.getActive())) {
            throw new BusinessValidationException("Selected variant is not available");
        }

        // Find existing cart item for same user + variant
        CartItem item = cartItemRepository.findByUserAndVariant(user, variant)
                .orElse(CartItem.builder()
                        .user(user)
                        .product(product)
                        .variant(variant)
                        .quantity(0)
                        .build());

        int newQty = item.getQuantity() + dto.getQuantity();
        if (variant.getStockQuantity() < newQty) {
            throw new BusinessValidationException(
                    "Only " + variant.getStockQuantity() + " items available for " + variant.getVariantLabel());
        }

        item.setQuantity(newQty);
        cartItemRepository.save(item);

        userActivityService.recordActivity(userId, productId, ActivityType.ADD_TO_CART,
                "Variant: " + variant.getSku() + " Qty: " + dto.getQuantity());

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

    public CartResponseDTO getCart(Long userId) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<CartItem> items = cartItemRepository.findByUser(user);
        List<CartItemResponseDTO> list = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : items) {
            Product p = item.getProduct();
            ProductVariant variant = item.getVariant();

            BigDecimal unitPrice;
            if (variant != null) {
                unitPrice = variant.getEffectivePrice(
                    Boolean.TRUE.equals(p.getOnSale()) && p.getSalePrice() != null ? p.getSalePrice() : p.getPrice()
                );
            } else {
                unitPrice = Boolean.TRUE.equals(p.getOnSale()) && p.getSalePrice() != null
                        ? p.getSalePrice() : p.getPrice();
            }

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            String image = p.getImages() != null && !p.getImages().isEmpty()
                    ? p.getImages().get(0).getImagePath() : null;

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
                    .stockQuantity(stock)
                    .variantLabel(variantLabel)
                    .variantAttributes(attrMap)
                    .build());
        }

        return CartResponseDTO.builder()
                .subtotal(subtotal.doubleValue())
                .items(list)
                .build();
    }
}