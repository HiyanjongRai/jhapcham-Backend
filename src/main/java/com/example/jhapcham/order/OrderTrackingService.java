package com.example.jhapcham.order;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderTrackingService {

    private final OrderTrackingRepository trackingRepo;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepository;

    public void addTracking(Long orderId, OrderTrackingStage stage, String message, BranchName branch) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderTracking t = OrderTracking.builder()
                .order(order)
                .stage(stage)
                .message(message)
                .branch(branch)
                .build();

        trackingRepo.save(t);

        if (stage == OrderTrackingStage.PROCESSING) {
            order.setStatus(OrderStatus.PROCESSING);
        }

        if (stage == OrderTrackingStage.SHIPPED) {
            order.setStatus(OrderStatus.SHIPPED);
        }

        if (stage == OrderTrackingStage.ARRIVED_AT_BRANCH) {
            order.setStatus(OrderStatus.SHIPPED);
        }

        if (stage == OrderTrackingStage.OUT_FOR_DELIVERY) {
            order.setStatus(OrderStatus.SHIPPED);
        }

        if (stage == OrderTrackingStage.DELIVERED) {
            order.setStatus(OrderStatus.DELIVERED);
        }

        if (stage == OrderTrackingStage.CANCELLED) {

            order.setStatus(OrderStatus.CANCELLED);

            // RESTORE STOCK ON CANCEL
            for (OrderItem item : order.getItems()) {
                Product p = item.getProduct();
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
            }
        }

        orderRepo.save(order);
    }

    public List<OrderTracking> getOrderTracking(Long orderId) {
        return trackingRepo.findByOrder_IdOrderByUpdateTimeAsc(orderId);
    }

    public List<OrderTracking> getAllTrackingForUser(Long userId) {
        return trackingRepo.getAllTrackingForUser(userId);
    }

    public OrderTracking getTrackingById(Long id) {
        return trackingRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tracking not found"));
    }


}