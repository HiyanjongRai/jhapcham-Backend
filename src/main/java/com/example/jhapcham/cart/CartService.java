package com.example.jhapcham.cart;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addToCart(Long userId, Long productId, int quantity, String color, String storage) {

        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be > 0");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() != Role.CUSTOMER)
            throw new IllegalStateException("Only customers can use the cart");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (product.getStatus() != Product.Status.ACTIVE || !product.isVisible())
            throw new IllegalStateException("Product is not available");

        if (product.getStock() < quantity)
            throw new IllegalStateException("Insufficient stock");

        if (color != null && !color.isBlank()
                && (product.getColors() == null || !product.getColors().contains(color))) {
            throw new IllegalArgumentException("Color not available");
        }

        if (storage != null && !storage.isBlank()
                && (product.getStorage() == null || !product.getStorage().contains(storage))) {
            throw new IllegalArgumentException("Storage not available");
        }

        Optional<CartItem> existing = cartItemRepository
                .findByUserAndProductAndSelectedColorAndSelectedStorage(user, product, color, storage);

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQty = item.getQuantity() + quantity;
            if (product.getStock() < newQty) throw new IllegalStateException("Insufficient stock");
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        } else {
            CartItem item = new CartItem();
            item.setUser(user);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setSelectedColor(color);
            item.setSelectedStorage(storage);
            cartItemRepository.save(item);
        }
    }

    public List<CartItemDto> getCartItems(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<CartItem> items = cartItemRepository.findByUser(user);

        return items.stream().map(ci -> {
            Product p = ci.getProduct();
            double unit = p.getPrice() == null ? 0.0 : p.getPrice();
            return new CartItemDto(
                    p.getId(),
                    p.getName(),
                    "/product-images/" + p.getImagePath(),
                    unit,
                    ci.getQuantity(),
                    unit * ci.getQuantity(),
                    ci.getSelectedColor(),
                    ci.getSelectedStorage(),
                    p.getCategory(),
                    p.getBrand()
            );

        }).collect(Collectors.toList());
    }

    @Transactional
    public void updateQuantity(Long userId, Long productId, int quantity, String color, String storage) {

        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be > 0");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        CartItem item = cartItemRepository
                .findByUserAndProductAndSelectedColorAndSelectedStorage(user, product, color, storage)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (product.getStock() < quantity)
            throw new IllegalStateException("Insufficient stock");

        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    @Transactional
    public void removeCartItem(Long userId, Long productId, String color, String storage) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        cartItemRepository.deleteByUserAndProductAndSelectedColorAndSelectedStorage(user, product, color, storage);
    }

    @Transactional
    public void clearCart(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        cartItemRepository.deleteAllByUser(user);
    }
}
