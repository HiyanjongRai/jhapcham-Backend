package com.example.jhapcham.order;

import com.example.jhapcham.cart.CartItem;
import com.example.jhapcham.cart.CartItemRepository;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;

    /* ----------------------- Public API ----------------------- */

    public ResponseEntity<?> placeOrderSingle(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            return ResponseEntity.badRequest().body("Quantity must be >= 1");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return notFound("User", userId);

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return notFound("Product", productId);

        // Stock check + decrement
        if (product.getStock() < quantity) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Insufficient stock. Available: " + product.getStock());
        }
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        // Build order and item
        Order order = Order.builder()
                .customer(user)
                .status(OrderStatus.PENDING)
                .totalPrice(0.0)
                .createdAt(LocalDateTime.now())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(quantity)
                .unitPrice(product.getPrice())
                .build();

        order.addItem(item);
        order.setTotalPrice(item.lineTotal());

        Order saved = orderRepository.save(order);
        logStatus(saved, OrderStatus.PENDING);

        return ResponseEntity.status(HttpStatus.CREATED).body(toSummaryDTO(saved));
    }

    public ResponseEntity<?> placeOrderFromCart(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return notFound("User", userId);

        List<CartItem> cart = cartItemRepository.findByUser(user);
        if (cart.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cart is empty");
        }

        // 1) Pre-validate stock for all items
        for (CartItem ci : cart) {
            Product p = ci.getProduct();
            if (p.getStock() < ci.getQuantity()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Insufficient stock for product " + p.getName() + ". Available: " + p.getStock());
            }
        }

        // 2) Decrement stock
        for (CartItem ci : cart) {
            Product p = ci.getProduct();
            p.setStock(p.getStock() - ci.getQuantity());
            productRepository.save(p);
        }

        // 3) Build order + items
        Order order = Order.builder()
                .customer(user)
                .status(OrderStatus.PENDING)
                .totalPrice(0.0)
                .createdAt(LocalDateTime.now())
                .build();

        double total = 0.0;
        for (CartItem ci : cart) {
            Product p = ci.getProduct();
            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .product(p)
                    .quantity(ci.getQuantity())
                    .unitPrice(p.getPrice())
                    .build();
            order.addItem(oi);
            total += oi.lineTotal();
        }
        order.setTotalPrice(total);

        Order saved = orderRepository.save(order);
        logStatus(saved, OrderStatus.PENDING);

        // 4) Clear cart
        cartItemRepository.deleteAll(cart);

        return ResponseEntity.status(HttpStatus.CREATED).body(toSummaryDTO(saved));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getUserOrders(Long userId) {
        if (!userRepository.existsById(userId)) return notFound("User", userId);
        List<Order> orders = orderRepository.findByCustomer_Id(userId);
        return ResponseEntity.ok(orders.stream().map(this::toSummaryDTO).toList());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getSellerOrders(Long sellerId) {
        List<Order> orders = orderRepository.findSellerOrders(sellerId);
        return ResponseEntity.ok(orders.stream().map(this::toSummaryDTO).toList());
    }



    public ResponseEntity<?> updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return notFound("Order", orderId);

        OrderStatus old = order.getStatus();
        if (old == newStatus) {
            return ResponseEntity.ok(toSummaryDTO(order));
        }

        // Restock only on transition to CANCELLED (and only once)
        if (newStatus == OrderStatus.CANCELLED && old != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product p = item.getProduct();
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
            }
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
        logStatus(order, newStatus);

        return ResponseEntity.ok(toSummaryDTO(order));
    }

    /* ----------------------- Helpers ----------------------- */

    private void logStatus(Order order, OrderStatus status) {
        statusHistoryRepository.save(
                OrderStatusHistory.builder()
                        .order(order)
                        .status(status)
                        .changedAt(LocalDateTime.now())
                        .build()
        );
    }

    private ResponseEntity<String> notFound(String what, Long id) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(what + " not found: " + id);
    }

    private OrderSummaryDTO toSummaryDTO(Order o) {
        List<OrderItemDTO> items = new ArrayList<>();
        for (OrderItem i : o.getItems()) {
            Product p = i.getProduct();
            items.add(
                    OrderItemDTO.builder()
                            .productId(p.getId())
                            .productName(p.getName())
                            .imagePath(p.getImagePath())
                            .unitPrice(i.getUnitPrice())
                            .quantity(i.getQuantity())
                            .lineTotal(i.lineTotal())
                            .build()
            );
        }
        return OrderSummaryDTO.builder()
                .orderId(o.getId())
                .createdAt(o.getCreatedAt())
                .status(o.getStatus())
                .totalPrice(o.getTotalPrice())
                .items(items)
                .build();
    }

    public ResponseEntity<?> getOne(Long orderId) {
        var orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Order not found: " + orderId));
        }

        var order = orderOpt.get();
        return ResponseEntity.ok(toSummaryDTO(order));
    }

}
