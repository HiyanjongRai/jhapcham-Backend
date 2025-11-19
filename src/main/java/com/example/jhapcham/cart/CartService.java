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
    public void addToCart(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be > 0");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Optional: cart only for customers
        if (user.getRole() != Role.CUSTOMER) {
            throw new IllegalStateException("Only customers can use the cart");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Only ACTIVE & visible products can be added
        if (product.getStatus() != Product.Status.ACTIVE || !product.isVisible()) {
            throw new IllegalStateException("Product is not available for purchase");
        }

        // Optional: enforce stock (comment out if you don't manage stock at cart time)
        if (product.getStock() < quantity) {
            throw new IllegalStateException("Insufficient stock");
        }

        Optional<CartItem> existingItemOpt = cartItemRepository.findByUserAndProduct(user, product);
        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQty = existingItem.getQuantity() + quantity;

            if (product.getStock() < newQty) {
                throw new IllegalStateException("Insufficient stock for requested quantity");
            }

            existingItem.setQuantity(newQty);
            cartItemRepository.save(existingItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }
    }

    public List<CartItemDto> getCartItems(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<CartItem> cartItems = cartItemRepository.findByUser(user);

        return cartItems.stream().map(ci -> {
            Product p = ci.getProduct();
            double unit = p.getPrice() == null ? 0.0 : p.getPrice();
            return new CartItemDto(
                    p.getId(),
                    p.getName(),
                    "/product-images/" + p.getImagePath(),
                    unit,
                    ci.getQuantity(),
                    unit * ci.getQuantity()
            );
        }).collect(Collectors.toList());
    }

    @Transactional
    public void updateQuantity(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be > 0");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (product.getStock() < quantity) {
            throw new IllegalStateException("Insufficient stock for requested quantity");
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
    }

    @Transactional
    public void removeCartItem(Long userId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        cartItemRepository.deleteByUserAndProduct(user, product);
    }

    // Optional: clear entire cart
    @Transactional
    public void clearCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        cartItemRepository.findByUser(user).forEach(cartItemRepository::delete);
    }
}
