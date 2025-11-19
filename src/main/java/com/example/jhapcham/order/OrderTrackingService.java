package com.example.jhapcham.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderTrackingService {

    private final OrderTrackingRepository trackingRepo;
    private final OrderRepository orderRepo;

    public void addTracking(Long orderId, OrderTrackingStage stage, String message, BranchName branch) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderTracking t = new OrderTracking();
        t.setOrder(order);
        t.setStage(stage);
        t.setMessage(message);
        t.setBranch(branch);

        trackingRepo.save(t);
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