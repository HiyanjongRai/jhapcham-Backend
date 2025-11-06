package com.example.jhapcham.order;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    //  Place single product order
    public ResponseEntity<?> placeOrder(Long userId, Long productId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(" User not found with ID: " + userId);
        }

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(" Product not found with ID: " + productId);
        }

        Order order = Order.builder()
                .customer(user)
                .products(List.of(product))
                .totalPrice(product.getPrice())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);
        logStatus(order, OrderStatus.PENDING);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(" Order placed successfully for product: " + product.getName());
    }

    //  Multi-product checkout
    public ResponseEntity<?> placeMultiProductOrder(Long userId, List<Long> productIds) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(" User not found with ID: " + userId);
        }

        List<Product> products = productRepository.findAllById(productIds);
        if (products.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(" No products found for the provided IDs.");
        }

        double total = products.stream().mapToDouble(Product::getPrice).sum();

        Order order = Order.builder()
                .customer(user)
                .products(products)
                .totalPrice(total)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);
        logStatus(order, OrderStatus.PENDING);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(" Multi-product order placed successfully. Total items: " + products.size());
    }

    //  Get user orders
    public ResponseEntity<?> getUserOrders(Long userId) {
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(" User not found with ID: " + userId);
        }

        List<Order> orders = orderRepository.findByCustomerId(userId);
        if (orders.isEmpty()) {
            return ResponseEntity.ok("️ No orders found for this user.");
        }
        return ResponseEntity.ok(orders);
    }

    //  Get seller orders
    public ResponseEntity<?> getSellerOrders(Long sellerId) {
        List<Order> orders = orderRepository.findBySellerId(sellerId);
        if (orders.isEmpty()) {
            return ResponseEntity.ok(" No orders found for seller ID: " + sellerId);
        }
        return ResponseEntity.ok(orders);
    }

    //  Update order status
    public ResponseEntity<?> updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(" Order not found with ID: " + orderId);
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
        logStatus(order, newStatus);
        return ResponseEntity.ok(" Order status updated to: " + newStatus);
    }

    // Cancel order
    public ResponseEntity<?> cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(" Order not found with ID: " + orderId);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        logStatus(order, OrderStatus.CANCELLED);
        return ResponseEntity.ok(" Order cancelled successfully.");
    }

    //  Get total price
    public ResponseEntity<?> getOrderTotal(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(" Order not found with ID: " + orderId);
        }
        return ResponseEntity.ok(" Total Price: " + order.getTotalPrice());
    }

    //  Status history
    public ResponseEntity<?> getStatusHistory(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(" Order not found with ID: " + orderId);
        }

        List<OrderStatusHistory> history = statusHistoryRepository.findByOrderId(orderId);
        if (history.isEmpty()) {
            return ResponseEntity.ok(" No status history found for this order.");
        }
        return ResponseEntity.ok(history);
    }

    //  Seller stats (simple example)
    public ResponseEntity<?> getSellerStats(Long sellerId) {
        List<Order> orders = orderRepository.findBySellerId(sellerId);
        return ResponseEntity.ok(" Total orders for seller ID " + sellerId + ": " + orders.size());
    }

    //  Monthly report (for admin)
    public ResponseEntity<?> getMonthlyReport() {
        List<Order> all = orderRepository.findAll();
        if (all.isEmpty()) {
            return ResponseEntity.ok(" No orders found for this month.");
        }
        return ResponseEntity.ok(all);
    }

    // 🧠 Helper
    private void logStatus(Order order, OrderStatus status) {
        statusHistoryRepository.save(
                OrderStatusHistory.builder()
                        .order(order)
                        .status(status)
                        .changedAt(LocalDateTime.now())
                        .build()
        );
    }
}
