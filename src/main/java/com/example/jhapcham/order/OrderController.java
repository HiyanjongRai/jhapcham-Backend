package com.example.jhapcham.order;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderTrackingService trackingService;
    private final OrderRepository orderRepo;

    // CREATE ORDER FROM CHECKOUT
    @PostMapping("/from-checkout/{checkoutId}")
    public ResponseEntity<?> createOrderFromCheckout(@PathVariable Long checkoutId) {
        return ResponseEntity.ok(orderService.createOrderFromCheckout(checkoutId));
    }

    // USER ORDERS
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(orderRepo.findTop200ByCustomer_IdOrderByCreatedAtDesc(userId));
    }

    // SELLER ORDERS
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<?> getSellerOrders(@PathVariable Long sellerId) {
        return ResponseEntity.ok(orderRepo.findSellerOrders(sellerId));
    }

    // ADMIN ORDERS
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAdminAllOrders() {
        return ResponseEntity.ok(orderRepo.findAll());
    }

    // GET ONE ORDER
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOne(@PathVariable Long orderId) {
        return ResponseEntity.ok(
                orderRepo.findById(orderId)
                        .orElseThrow(() -> new RuntimeException("Order not found"))
        );
    }

    // UPDATE STATUS
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status
    ) {
        return ResponseEntity.ok(orderService.updateStatus(orderId, status));
    }

    // ADD TRACKING
    @PostMapping("/{orderId}/tracking")
    public ResponseEntity<?> addTrackingStage(
            @PathVariable Long orderId,
            @RequestParam OrderTrackingStage stage,
            @RequestParam(required = false) BranchName branch
    ) {

        String message = switch (stage) {
            case PROCESSING -> "Your order is now being processed";
            case SHIPPED -> "Your order was sent to branch";
            case ARRIVED_AT_BRANCH -> "Your order arrived at branch";
            case OUT_FOR_DELIVERY -> "Your order is out for delivery";
            case DELIVERED -> "Your order has been delivered";
            case CANCELLED -> "Your order has been cancelled";
        };

        trackingService.addTracking(orderId, stage, message, branch);

        return ResponseEntity.ok("Tracking stage added");
    }

    // TRACKING TIMELINE
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<?> getTracking(@PathVariable Long orderId) {
        return ResponseEntity.ok(trackingService.getOrderTracking(orderId));
    }

    // USER TRACKING LIST
    @GetMapping("/tracking/user/{userId}")
    public ResponseEntity<?> getTrackingForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(trackingService.getAllTrackingForUser(userId));
    }

    // TRACKING DETAILS
    @GetMapping("/tracking/details/{trackingId}")
    public ResponseEntity<?> getTrackingDetails(@PathVariable Long trackingId) {
        return ResponseEntity.ok(trackingService.getTrackingById(trackingId));
    }


    // CART CHECKOUT ORDER
    @PostMapping("/checkout")
    public ResponseEntity<?> placeOrderFromCart(
            @RequestParam Long userId,
            @RequestParam(required = false) String fullAddress,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng
    ) {

        boolean hasAddress = fullAddress != null && !fullAddress.isBlank();
        boolean hasCoords = lat != null && lng != null;

        if (!hasAddress && !hasCoords) {
            return ResponseEntity.badRequest().body("Provide address or coordinates");
        }

        return orderService.placeOrderFromCart(
                userId,
                fullAddress,
                lat,
                lng
        );
    }
}

