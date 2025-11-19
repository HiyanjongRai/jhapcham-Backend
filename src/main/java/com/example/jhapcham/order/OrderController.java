package com.example.jhapcham.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderTrackingService trackingService;
    private final OrderRepository orderRepo;

    /* --------------------------------------------------------
       PLACE ORDER
    --------------------------------------------------------- */
    @PostMapping("/place")
    public ResponseEntity<?> placeOrderSingle(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity) {

        return orderService.placeOrderSingle(userId, productId, quantity);
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> placeOrderFromCart(@RequestParam Long userId) {
        return orderService.placeOrderFromCart(userId);
    }

    /* --------------------------------------------------------
       GET ORDERS
    --------------------------------------------------------- */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(@PathVariable Long userId) {
        return orderService.getUserOrders(userId);
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<?> getSellerOrders(@PathVariable Long sellerId) {
        return orderService.getSellerOrders(sellerId);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAdminAllOrders() {
        return ResponseEntity.ok(orderRepo.findByStatus(OrderStatus.DELIVERED));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOne(@PathVariable Long orderId) {
        return orderService.getOne(orderId);
    }

    /* --------------------------------------------------------
       UPDATE ORDER STATUS ONLY
    --------------------------------------------------------- */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {

        return orderService.updateStatus(orderId, status);
    }

    /* --------------------------------------------------------
       ADD TRACKING ENTRY (movement)
    --------------------------------------------------------- */
    @PostMapping("/{orderId}/tracking")
    public ResponseEntity<?> addTrackingStage(
            @PathVariable Long orderId,
            @RequestParam OrderTrackingStage stage,
            @RequestParam(required = false) BranchName branch) {

        String message = buildTrackingMessage(stage);

        trackingService.addTracking(orderId, stage, message, branch);

        return ResponseEntity.ok("Tracking stage added");
    }

    /* --------------------------------------------------------
       GET ORDER TRACKING TIMELINE
    --------------------------------------------------------- */
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<?> getTracking(
            @PathVariable Long orderId,
            @RequestParam(required = false) Long userId) {

        if (userId != null) {
            var order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            if (!order.getCustomer().getId().equals(userId)) {
                return ResponseEntity.status(403).body("Access denied");
            }
        }

        return ResponseEntity.ok(trackingService.getOrderTracking(orderId));
    }

    /* --------------------------------------------------------
       MESSAGE BUILDER FOR TRACKING
    --------------------------------------------------------- */
    private String buildTrackingMessage(OrderTrackingStage stage) {
        return switch (stage) {
            case PROCESSING -> "Your order is now being processed";
            case SENT_TO_BRANCH -> "Your order was sent to branch";
            case ARRIVED_AT_BRANCH -> "Your order arrived at branch";
            case OUT_FOR_DELIVERY -> "Your order is out for delivery";
            case DELIVERED -> "Your order has been delivered";
            case CANCELLED -> "Your order has been cancelled";
        };
    }

    @GetMapping("/tracking/user/{userId}")
    public ResponseEntity<?> getTrackingForUser(@PathVariable Long userId) {

        var list = trackingService.getAllTrackingForUser(userId);

        var dtoList = list.stream()
                .map(t -> new OrderTrackingDTO(
                        t.getId(),
                        t.getStage().name(),
                        t.getMessage(),
                        t.getBranch() != null ? t.getBranch().name() : null,
                        t.getUpdateTime(),
                        t.getOrder().getId()
                ))
                .toList();

        return ResponseEntity.ok(dtoList);
    }


    @GetMapping("/tracking/details/{trackingId}")
    public ResponseEntity<?> getTrackingDetails(@PathVariable Long trackingId) {
        return ResponseEntity.ok(
                trackingService.getTrackingById(trackingId)
        );
    }

}
