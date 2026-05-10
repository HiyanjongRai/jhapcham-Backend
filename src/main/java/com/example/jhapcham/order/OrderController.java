package com.example.jhapcham.order;

import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestBody CheckoutRequestDTO dto, Authentication authentication) {
        try {
            if (dto.getUserId() != null) {
                currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), dto.getUserId());
            }
            return ResponseEntity.ok(orderService.previewOrder(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Preview failed: " + e.getMessage()));
        }
    }

    @PostMapping("/cart")
    public ResponseEntity<?> placeFromCart(@RequestBody CartCheckoutRequestDTO dto, Authentication authentication) {
        try {
            currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), dto.getUserId());
            return ResponseEntity.ok(orderService.placeOrderFromCart(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Cart checkout failed"));
        }
    }

    @PostMapping
    public ResponseEntity<?> place(@RequestBody CheckoutRequestDTO dto, Authentication authentication) {
        try {
            if (dto.getUserId() != null) {
                currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), dto.getUserId());
            }
            return ResponseEntity.ok(orderService.placeOrder(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Placement failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/retry-payment")
    public ResponseEntity<?> retryPayment(@PathVariable Long orderId, Authentication authentication) {
        try {
            return ResponseEntity.ok(orderService.retryPayment(orderId, currentUserService.requireUser(authentication)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Retry failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable Long orderId, Authentication authentication) {
        try {
            return ResponseEntity.ok(orderService.getOrder(orderId, currentUserService.requireUser(authentication)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Loading failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusUpdateRequest request,
            Authentication authentication) {
        try {
            return ResponseEntity.ok(
                    orderService.updateSellerOrderStatus(
                            orderId,
                            request.getStatus(),
                            currentUserService.requireUser(authentication)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Status update failed: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(@PathVariable Long userId, Authentication authentication) {
        try {
            currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
            return ResponseEntity.ok(orderService.getOrdersForUser(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Loading failed: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/list")
    public ResponseEntity<?> getUserOrdersSimple(@PathVariable Long userId, Authentication authentication) {
        try {
            currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
            return ResponseEntity.ok(orderService.getOrdersForUserSimple(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Loading failed: " + e.getMessage()));
        }
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<?> getSellerOrders(@PathVariable Long sellerId, Authentication authentication) {
        try {
            currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerId);
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
            @RequestBody AssignBranchDTO dto,
            Authentication authentication) {
        try {
            currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerId);
            return ResponseEntity.ok(orderService.sellerAssignBranch(orderId, sellerId, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Branch update failed: " + e.getMessage()));
        }
    }

    @PutMapping("/seller/{sellerId}/express-dispatch/{orderId}")
    public ResponseEntity<?> expressDispatch(
            @PathVariable Long sellerId,
            @PathVariable Long orderId,
            @RequestBody AssignBranchDTO dto,
            Authentication authentication) {
        try {
            currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerId);
            return ResponseEntity.ok(orderService.sellerExpressDispatch(orderId, sellerId, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Express dispatch failed: " + e.getMessage()));
        }
    }

    @PutMapping("/branch/{orderId}/status")
    public ResponseEntity<?> branchUpdateStatus(
            @PathVariable Long orderId,
            @RequestParam DeliveryBranch branch,
            @RequestParam OrderStatus nextStatus,
            Authentication authentication) {
        currentUserService.requireAdmin(currentUserService.requireUser(authentication));
        return ResponseEntity.ok(
                orderService.branchUpdateStatus(orderId, branch.name(), nextStatus.name()));
    }

    @PutMapping("/user/{userId}/cancel/{orderId}")
    public ResponseEntity<?> customerCancel(
            @PathVariable Long userId,
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
            return ResponseEntity.ok(orderService.customerCancelOrder(orderId, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Cancel failed: " + e.getMessage()));
        }
    }

    @PutMapping("/guest/cancel/{orderId}")
    public ResponseEntity<?> guestCancel(@PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(orderService.guestCancelOrder(orderId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Guest cancel failed: " + e.getMessage()));
        }
    }

    @PutMapping("/seller/{sellerId}/process/{orderId}")
    public ResponseEntity<?> processOrder(
            @PathVariable Long sellerId,
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerId);
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
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerId);
            return ResponseEntity.ok(orderService.sellerCancelOrder(orderId, sellerId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Cancel failed: " + e.getMessage()));
        }
    }

    @PostMapping("/branch/{orderId}/verify-otp")
    public ResponseEntity<?> verifyDeliveryOtp(
            @PathVariable Long orderId,
            @RequestParam DeliveryBranch branch,
            @RequestParam String otp,
            Authentication authentication) {
        try {
            currentUserService.requireAdmin(currentUserService.requireUser(authentication));
            return ResponseEntity.ok(orderService.verifyDeliveryOtp(orderId, branch.name(), otp));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("OTP verification failed: " + e.getMessage()));
        }
    }

    @PostMapping("/branch/{orderId}/resend-otp")
    public ResponseEntity<?> resendDeliveryOtp(
            @PathVariable Long orderId,
            @RequestParam DeliveryBranch branch,
            Authentication authentication) {
        try {
            currentUserService.requireAdmin(currentUserService.requireUser(authentication));
            orderService.resendDeliveryOtp(orderId, branch.name());
            return ResponseEntity.ok("OTP resent successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("OTP resend failed: " + e.getMessage()));
        }
    }
}
