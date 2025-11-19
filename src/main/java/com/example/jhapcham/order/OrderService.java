package com.example.jhapcham.order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    @PersistenceContext
    private EntityManager em;



    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;

    /* -------------------------------------------------------
       PLACE ORDER SINGLE PRODUCT
    ------------------------------------------------------- */
    public ResponseEntity<OrderSummaryDTO> placeOrderSingle(Long userId, Long productId, int qty) {

        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return ResponseEntity.status(404).body(null);

        Product p = productRepository.findById(productId).orElse(null);
        if (p == null)
            return ResponseEntity.status(404).body(null);

        if (p.getStock() < qty)
            return ResponseEntity.status(409).body(null);

        p.setStock(p.getStock() - qty);
        productRepository.save(p);

        Order order = Order.builder()
                .customer(user)
                .status(OrderStatus.PENDING)
                .totalPrice(0.0)
                .createdAt(LocalDateTime.now())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(p)
                .quantity(qty)
                .unitPrice(p.getPrice())
                .build();

        order.addItem(item);
        order.setTotalPrice(item.lineTotal());

        Order saved = orderRepository.save(order);
        saveHistory(saved, OrderStatus.PENDING);

        return ResponseEntity.status(201).body(toDTO(saved));
    }

    /* -------------------------------------------------------
       PLACE ORDER FROM CART
    ------------------------------------------------------- */
    public ResponseEntity<OrderSummaryDTO> placeOrderFromCart(Long userId) {

        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return ResponseEntity.status(404).body(null);

        List<CartItem> cart = cartItemRepository.findByUser(user);
        if (cart.isEmpty())
            return ResponseEntity.status(400).body(null);

        for (CartItem ci : cart) {
            if (ci.getProduct().getStock() < ci.getQuantity())
                return ResponseEntity.status(409).body(null);
        }

        for (CartItem ci : cart) {
            Product p = ci.getProduct();
            p.setStock(p.getStock() - ci.getQuantity());
            productRepository.save(p);
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

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(p)
                    .quantity(ci.getQuantity())
                    .unitPrice(p.getPrice())
                    .build();

            order.addItem(item);
            total += item.lineTotal();
        }

        order.setTotalPrice(total);

        Order saved = orderRepository.save(order);
        saveHistory(saved, OrderStatus.PENDING);

        cartItemRepository.deleteAll(cart);

        return ResponseEntity.status(201).body(toDTO(saved));
    }

    /* -------------------------------------------------------
       GET USER ORDERS
    ------------------------------------------------------- */
    @Transactional(readOnly = true)
    public ResponseEntity<List<OrderSummaryDTO>> getUserOrders(Long userId) {

        if (!userRepository.existsById(userId))
            return ResponseEntity.status(404).body(null);

        List<Order> orders = orderRepository.findByCustomer_Id(userId);

        return ResponseEntity.ok(orders.stream().map(this::toDTO).toList());
    }

    /* -------------------------------------------------------
       GET SELLER ORDERS
    ------------------------------------------------------- */
    @Transactional(readOnly = true)
    public ResponseEntity<List<OrderSummaryDTO>> getSellerOrders(Long sellerId) {

        List<Order> orders = orderRepository.findSellerOrders(sellerId);

        return ResponseEntity.ok(orders.stream().map(this::toDTO).toList());
    }

    /* -------------------------------------------------------
       GET ONE ORDER
    ------------------------------------------------------- */
    public ResponseEntity<OrderSummaryDTO> getOne(Long orderId) {

        Order order = orderRepository.findById(orderId).orElse(null);

        if (order == null)
            return ResponseEntity.status(404).body(null);

        return ResponseEntity.ok(toDTO(order));
    }

    /* -------------------------------------------------------
       UPDATE ORDER STATUS
    ------------------------------------------------------- */
    public ResponseEntity<OrderSummaryDTO> updateStatus(Long orderId, OrderStatus newStatus) {

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null)
            return ResponseEntity.status(404).body(null);

        OrderStatus oldStatus = order.getStatus();

        if (newStatus == OrderStatus.CANCELLED && oldStatus != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product p = item.getProduct();
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
            }
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
        saveHistory(order, newStatus);

        // ONLY for delivered status
        if (newStatus == OrderStatus.DELIVERED) {

            OrderTracking reviewNotify = OrderTracking.builder()
                    .order(order)
                    .stage(OrderTrackingStage.DELIVERED)
                    .message("Your order " + order.getId() + " is delivered. Please submit your review.")
                    .updateTime(LocalDateTime.now())
                    .build();

            em.persist(reviewNotify);
        }

        return ResponseEntity.ok(toDTO(order));
    }


    /* -------------------------------------------------------
       SAVE STATUS HISTORY
    ------------------------------------------------------- */
    private void saveHistory(Order order, OrderStatus status) {
        historyRepository.save(
                OrderStatusHistory.builder()
                        .order(order)
                        .status(status)
                        .changedAt(LocalDateTime.now())
                        .build()
        );
    }

    /* -------------------------------------------------------
       DTO CONVERTER
    ------------------------------------------------------- */
    private OrderSummaryDTO toDTO(Order order) {

        List<OrderItemDTO> itemDTOs = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            Product p = item.getProduct();

            itemDTOs.add(
                    OrderItemDTO.builder()
                            .productId(p.getId())
                            .productName(p.getName())
                            .imagePath(p.getImagePath())
                            .unitPrice(item.getUnitPrice())
                            .quantity(item.getQuantity())
                            .lineTotal(item.lineTotal())
                            .build()
            );
        }

        return OrderSummaryDTO.builder()
                .orderId(order.getId())
                .createdAt(order.getCreatedAt())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .items(itemDTOs)
                .build();
    }

}
