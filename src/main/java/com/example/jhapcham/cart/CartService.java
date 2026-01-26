package com.example.jhapcham.cart;

import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.activity.ActivityType;
import com.example.jhapcham.activity.UserActivityService;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductImage;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CartService {

        private final CartItemRepository cartItemRepository;
        private final ProductRepository productRepository;
        private final UserRepository userRepository;
        private final UserActivityService userActivityService;

        @Transactional
        public CartResponseDTO addToCart(
                        Long userId,
                        Long productId,
                        AddToCartRequestDTO dto) {

                if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
                        throw new BusinessValidationException("Quantity must be greater than zero");
                }

                User user = userRepository.findById(Objects.requireNonNull(userId, "User ID cannot be null"))
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Product product = productRepository
                                .findById(Objects.requireNonNull(productId, "Product ID cannot be null"))
                                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

                CartItem item = cartItemRepository.findByUserAndProduct(user, product)
                                .orElse(
                                                CartItem.builder()
                                                                .user(user)
                                                                .product(product)
                                                                .quantity(0)
                                                                .build());

                item.setQuantity(item.getQuantity() + dto.getQuantity());
                item.setSelectedColor(dto.getSelectedColor());
                item.setSelectedStorage(dto.getSelectedStorage());

                cartItemRepository.save(item);

                // Unified activity logging
                userActivityService.recordActivity(userId, productId, ActivityType.ADD_TO_CART,
                                "Qty: " + dto.getQuantity());

                return getCart(userId);

        }

        @Transactional
        public CartResponseDTO updateQuantity(Long userId, Long cartItemId, Integer qty) {

                if (qty == null) {
                        throw new BusinessValidationException("Quantity is required");
                }

                CartItem item = cartItemRepository
                                .findById(Objects.requireNonNull(cartItemId, "Cart item ID cannot be null"))
                                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

                if (!item.getUser().getId().equals(userId)) {
                        throw new AuthorizationException("You are not allowed to modify this cart item");
                }

                if (qty <= 0) {
                        cartItemRepository.delete(item);
                } else {
                        item.setQuantity(qty);
                        cartItemRepository.save(item);
                }

                return getCart(userId);
        }

        public CartResponseDTO getCart(Long userId) {

                User user = userRepository.findById(Objects.requireNonNull(userId, "User ID cannot be null"))
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                List<CartItem> items = cartItemRepository.findByUser(user);

                List<CartItemResponseDTO> list = new ArrayList<>();
                BigDecimal subtotal = BigDecimal.ZERO;

                for (CartItem item : items) {

                        Product p = item.getProduct();

                        BigDecimal unitPrice = Boolean.TRUE.equals(p.getOnSale()) && p.getSalePrice() != null
                                        ? p.getSalePrice()
                                        : p.getPrice();

                        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

                        subtotal = subtotal.add(lineTotal);

                        String image = p.getImages() != null && !p.getImages().isEmpty()
                                        ? p.getImages().stream().findFirst().map(ProductImage::getImagePath).orElse(null)
                                        : null;

                        list.add(
                                        CartItemResponseDTO.builder()
                                                        .cartItemId(item.getId())
                                                        .productId(p.getId())
                                                        .name(p.getName())
                                                        .brand(p.getBrand())
                                                        .image(image)
                                                        .quantity(item.getQuantity())
                                                        .price(unitPrice)
                                                        .selectedColor(item.getSelectedColor())
                                                        .selectedStorage(item.getSelectedStorage())
                                                        .build());
                }

                return CartResponseDTO.builder()
                                .subtotal(subtotal.doubleValue())
                                .items(list)
                                .build();

        }

}