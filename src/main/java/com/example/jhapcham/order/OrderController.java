package com.example.jhapcham.order;

import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestBody CheckoutRequestDTO dto) {
        try {
            return ResponseEntity.ok(orderService.previewOrder(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Preview failed: " + e.getMessage()));
        }
    }

    @PostMapping("/cart")
    public ResponseEntity<?> placeFromCart(@RequestBody CartCheckoutRequestDTO dto) {
        try {
            return ResponseEntity.ok(orderService.placeOrderFromCart(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Cart checkout failed"));
        }
    }

    @PostMapping
    public ResponseEntity<?> place(@RequestBody CheckoutRequestDTO dto) {
        try {
            return ResponseEntity.ok(orderService.placeOrder(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Placement failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(orderService.getOrder(orderId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Loading failed: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(orderService.getOrdersForUser(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Loading failed: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/list")
    public ResponseEntity<?> getUserOrdersSimple(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(orderService.getOrdersForUserSimple(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Loading failed: " + e.getMessage()));
        }
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<?> getSellerOrders(@PathVariable Long sellerId) {
        try {
            return ResponseEntity.ok(orderService.getOrdersForSeller(sellerId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Loading failed: " + e.getMessage()));
        }
    }

    @PutMapping("/seller/{sellerId}/assign/{orderId}")
    public ResponseEntity<?> assignBranch(
            @PathVariable Long sellerId,
            @PathVariable Long orderId,
            @RequestBody AssignBranchDTO dto) {
        try {
            return ResponseEntity.ok(orderService.sellerAssignBranch(orderId, sellerId, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Branch update failed: " + e.getMessage()));
        }
    }

    @PutMapping("/branch/{orderId}/status")
    public ResponseEntity<?> branchUpdateStatus(
            @PathVariable Long orderId,
            @RequestParam DeliveryBranch branch,
            @RequestParam OrderStatus nextStatus) {
        return ResponseEntity.ok(
                orderService.branchUpdateStatus(orderId, branch.name(), nextStatus.name()));
    }

    @PutMapping("/user/{userId}/cancel/{orderId}")
    public ResponseEntity<?> customerCancel(
            @PathVariable Long userId,
            @PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(orderService.customerCancelOrder(orderId, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Cancel failed: " + e.getMessage()));
        }
    }

    @PutMapping("/seller/{sellerId}/process/{orderId}")
    public ResponseEntity<?> processOrder(
            @PathVariable Long sellerId,
            @PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(orderService.sellerProcessOrder(orderId, sellerId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Processing failed: " + e.getMessage()));
        }
    }

    @PutMapping("/seller/{sellerId}/cancel/{orderId}")
    public ResponseEntity<?> sellerCancel(
            @PathVariable Long sellerId,
            @PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(orderService.sellerCancelOrder(orderId, sellerId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Cancel failed: " + e.getMessage()));
        }
    }
}
