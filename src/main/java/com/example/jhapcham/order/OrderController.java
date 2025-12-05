package com.example.jhapcham.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderTrackingService trackingService;
    private final OrderRepository orderRepo;

    /* --------------------------------------------------------
       SINGLE PRODUCT ORDER
    --------------------------------------------------------- */
    @PostMapping("/place")
    public ResponseEntity<?> placeOrderSingle(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(required = false) String fullAddress,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {

        boolean hasAddress = fullAddress != null && !fullAddress.isBlank();
        boolean hasCoords = lat != null && lng != null;

        if (!hasAddress && !hasCoords)
            return ResponseEntity.badRequest().body("Provide address or coordinates");

        return orderService.placeOrderSingle(userId, productId, quantity, fullAddress, lat, lng);
    }

    /* --------------------------------------------------------
       CART CHECKOUT ORDER
    --------------------------------------------------------- */
    @PostMapping("/checkout")
    public ResponseEntity<?> placeOrderFromCart(
            @RequestParam Long userId,
            @RequestParam(required = false) String fullAddress,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {

        boolean hasAddress = fullAddress != null && !fullAddress.isBlank();
        boolean hasCoords = lat != null && lng != null;

        if (!hasAddress && !hasCoords)
            return ResponseEntity.badRequest().body("Provide address or coordinates");

        return orderService.placeOrderFromCart(userId, fullAddress, lat, lng);
    }

    /* --------------------------------------------------------
       BUY NOW ORDER
    --------------------------------------------------------- */
    @PostMapping("/buy-now")
    public ResponseEntity<?> buyNow(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(required = false) String fullAddress,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {

        boolean hasAddress = fullAddress != null && !fullAddress.isBlank();
        boolean hasCoords = lat != null && lng != null;

        if (!hasAddress && !hasCoords)
            return ResponseEntity.badRequest().body("Provide address or coordinates");

        return orderService.placeOrderSingle(userId, productId, quantity, fullAddress, lat, lng);
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
        return ResponseEntity.ok(orderRepo.findAll());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOne(@PathVariable Long orderId) {
        return orderService.getOne(orderId);
    }

    /* --------------------------------------------------------
       UPDATE ORDER STATUS
    --------------------------------------------------------- */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {

        return orderService.updateStatus(orderId, status);
    }

    /* --------------------------------------------------------
       ADD TRACKING (Movement)
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
       GET TRACKING TIMELINE
    --------------------------------------------------------- */
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<?> getTracking(
            @PathVariable Long orderId,
            @RequestParam(required = false) Long userId) {

        if (userId != null) {
            var order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            if (!order.getCustomer().getId().equals(userId))
                return ResponseEntity.status(403).body("Access denied");
        }

        return ResponseEntity.ok(trackingService.getOrderTracking(orderId));
    }

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

    /* --------------------------------------------------------
       TRACKING LIST FOR USER
    --------------------------------------------------------- */
        /* --------------------------------------------------------
       TRACKING LIST FOR USER
    --------------------------------------------------------- */
    @GetMapping("/tracking/user/{userId}")
    public ResponseEntity<?> getTrackingForUser(@PathVariable Long userId) {

        var list = trackingService.getAllTrackingForUser(userId);

        var dtoList = list.stream()
                .map(t -> {
                    // Fetch order items and convert to DTOs
                    List<OrderItemDTO> itemDTOs = t.getOrder().getItems().stream()
                            .map(item -> OrderItemDTO.builder()
                                    .productId(item.getProduct().getId())
                                    .productName(item.getProduct().getName())
                                    .imagePath(item.getProduct().getImagePath())
                                    .unitPrice(item.getUnitPrice())
                                    .quantity(item.getQuantity())
                                    .lineTotal(item.lineTotal())
                                    .selectedColor(item.getSelectedColor())
                                    .selectedStorage(item.getSelectedStorage())
                                    .build())
                            .toList();

                    return new OrderTrackingDTO(
                            t.getId(),
                            t.getStage().name(),
                            t.getMessage(),
                            t.getBranch() != null ? t.getBranch().name() : null,
                            t.getUpdateTime(),
                            t.getOrder().getId(),
                            itemDTOs, // Pass items here
                            t.getOrder().getTotalPrice(),
                            t.getOrder().getCreatedAt()
                    );
                })
                .toList();

        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/tracking/details/{trackingId}")
    public ResponseEntity<?> getTrackingDetails(@PathVariable Long trackingId) {
        return ResponseEntity.ok(trackingService.getTrackingById(trackingId));
    }
}
