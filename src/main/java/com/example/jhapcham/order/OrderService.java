package com.example.jhapcham.order;

import com.example.jhapcham.Checkout.CheckoutSession;
import com.example.jhapcham.Checkout.CheckoutSessionRepository;
import com.example.jhapcham.Checkout.PaymentMethod;
import com.example.jhapcham.cart.CartItem;
import com.example.jhapcham.cart.CartItemRepository;
import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    @PersistenceContext
    private EntityManager em;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final CheckoutSessionRepository checkoutRepo;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;

    // FROM CHECKOUT
    public Order createOrderFromCheckout(Long checkoutId) {

        CheckoutSession checkout = checkoutRepo.findById(checkoutId)
                .orElseThrow(() -> new IllegalStateException("Checkout not found"));

        if (checkout.getPaymentMethod() == PaymentMethod.ONLINE && !checkout.getIsPaid()) {
            throw new IllegalStateException("Checkout not paid");
        }

        User user = userRepository.findById(checkout.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        OrderStatus status = checkout.getPaymentMethod() == PaymentMethod.COD
                ? OrderStatus.PENDING
                : OrderStatus.PROCESSING;

        Order order = Order.builder()
                .customer(user)
                .createdAt(LocalDateTime.now())
                .status(status)
                .totalPrice(checkout.getGrandTotal())
                .build();

        checkout.getItems().forEach(snap -> {

            Product product = productRepository.findById(snap.getProductId())
                    .orElseThrow();

            if (product.getStock() < snap.getQuantity()) {
                throw new IllegalStateException("Stock not enough for " + product.getName());
            }

            product.setStock(product.getStock() - snap.getQuantity());
            productRepository.save(product);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(snap.getQuantity())
                    .unitPrice(snap.getUnitPrice())
                    .selectedColor(snap.getSelectedColor())
                    .selectedStorage(snap.getSelectedStorage())
                    .build();

            order.addItem(item);
        });

        Order saved = orderRepository.save(order);

        historyRepository.save(
                OrderStatusHistory.builder()
                        .order(saved)
                        .status(saved.getStatus())
                        .changedAt(LocalDateTime.now())
                        .build()
        );

        return saved;


    }
    // SINGLE PRODUCT ORDER
    public ResponseEntity<?> placeOrderSingle(Long userId, Long productId, int qty,
                                              String fullAddress, Double lat, Double lng) {

        User user = userRepository.findById(userId).orElse(null);
        Product p = productRepository.findById(productId).orElse(null);

        if (user == null || p == null || p.getStock() < qty) {
            return ResponseEntity.badRequest().body("Invalid request");
        }

        p.setStock(p.getStock() - qty);
        productRepository.save(p);

        Order order = Order.builder()
                .customer(user)
                .status(OrderStatus.PENDING)
                .totalPrice(p.getPrice() * qty)
                .createdAt(LocalDateTime.now())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(p)
                .quantity(qty)
                .unitPrice(p.getPrice())
                .build();

        order.addItem(item);

        Order saved = orderRepository.save(order);

        historyRepository.save(
                OrderStatusHistory.builder()
                        .order(saved)
                        .status(OrderStatus.PENDING)
                        .changedAt(LocalDateTime.now())
                        .build()
        );

        return ResponseEntity.ok(saved);
    }

    // CART ORDER
    public ResponseEntity<?> placeOrderFromCart(Long userId,
                                                String fullAddress,
                                                Double lat,
                                                Double lng) {

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("User not found");
        }

        List<CartItem> cart = cartItemRepository.findByUser(user);
        if (cart.isEmpty()) {
            return ResponseEntity.badRequest().body("Cart empty");
        }

        Order order = Order.builder()
                .customer(user)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .totalPrice(0.0)
                .build();

        double total = 0.0;

        for (CartItem ci : cart) {
            Product p = ci.getProduct();

            if (p.getStock() < ci.getQuantity()) {
                return ResponseEntity.badRequest().body("Stock issue");
            }

            p.setStock(p.getStock() - ci.getQuantity());
            productRepository.save(p);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(p)
                    .quantity(ci.getQuantity())
                    .unitPrice(p.getPrice())
                    .selectedColor(ci.getSelectedColor())
                    .selectedStorage(ci.getSelectedStorage())
                    .build();

            order.addItem(item);
            total += item.lineTotal();
        }

        order.setTotalPrice(total);

        Order saved = orderRepository.save(order);

        cartItemRepository.deleteAll(cart);

        historyRepository.save(
                OrderStatusHistory.builder()
                        .order(saved)
                        .status(OrderStatus.PENDING)
                        .changedAt(LocalDateTime.now())
                        .build()
        );

        return ResponseEntity.ok(saved);
    }

    // USER ORDERS
    public ResponseEntity<?> getUserOrders(Long userId) {
        return ResponseEntity.ok(orderRepository.findByCustomer_Id(userId));
    }

    // SELLER ORDERS
    public ResponseEntity<?> getSellerOrders(Long sellerId) {
        return ResponseEntity.ok(orderRepository.findSellerOrders(sellerId));
    }

    // GET ONE
    public ResponseEntity<?> getOne(Long orderId) {
        return ResponseEntity.ok(
                orderRepository.findById(orderId)
                        .orElseThrow(() -> new IllegalStateException("Order not found"))
        );
    }

    // UPDATE STATUS
    public ResponseEntity<?> updateStatus(Long orderId, OrderStatus status) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found"));

        // If cancelling, restore product stock
        if (status == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {

            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setStatus(status);
        orderRepository.save(order);

        historyRepository.save(
                OrderStatusHistory.builder()
                        .order(order)
                        .status(status)
                        .changedAt(LocalDateTime.now())
                        .build()
        );

        return ResponseEntity.ok(order);
    }



}