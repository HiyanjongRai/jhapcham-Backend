package com.example.jhapcham.Cart;

import com.example.jhapcham.Cart.CartItemRepository;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartResponseDTO addToCart(
            Long userId,
            Long productId,
            AddToCartRequestDTO dto
    ) {

        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than zero");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        CartItem item = cartItemRepository.findByUserAndProduct(user, product)
                .orElse(
                        CartItem.builder()
                                .user(user)
                                .product(product)
                                .quantity(0)
                                .build()
                );

        item.setQuantity(item.getQuantity() + dto.getQuantity());
        item.setSelectedColor(dto.getSelectedColor());
        item.setSelectedStorage(dto.getSelectedStorage());

        cartItemRepository.save(item);
        return getCart(userId);


    }

    @Transactional
    public CartResponseDTO updateQuantity(Long userId, Long cartItemId, Integer qty) {

        if (qty == null) {
            throw new RuntimeException("Quantity is required");
        }

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not allowed to modify this cart item");
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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<CartItem> items = cartItemRepository.findByUser(user);

        List<CartItemResponseDTO> list = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : items) {

            Product p = item.getProduct();

            BigDecimal unitPrice =
                    Boolean.TRUE.equals(p.getOnSale()) && p.getSalePrice() != null
                            ? p.getSalePrice()
                            : p.getPrice();

            BigDecimal lineTotal =
                    unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            subtotal = subtotal.add(lineTotal);

            String image = p.getImages() != null && !p.getImages().isEmpty()
                    ? p.getImages().get(0).getImagePath()
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
                            .build()
            );
        }

        return CartResponseDTO.builder()
                .subtotal(subtotal.doubleValue())
                .items(list)
                .build();


    }

}