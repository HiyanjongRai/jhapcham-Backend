package com.example.jhapcham.order.api;


import com.example.jhapcham.order.application.*;
import com.example.jhapcham.order.domain.*;
import com.example.jhapcham.order.dto.*;
import com.example.jhapcham.order.persistence.*;
import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.Error.OrderStateConflictException;
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
            rethrowApiException(e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Retry failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{orderId}/payment-method")
    public ResponseEntity<?> changePaymentMethod(
            @PathVariable Long orderId,
            @RequestParam String method,
            Authentication authentication) {
        try {
            return ResponseEntity.ok(orderService.changePaymentMethod(
                    orderId, method, currentUserService.requireUser(authentication)));
        } catch (RuntimeException e) {
            rethrowApiException(e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Payment method change failed: " + e.getMessage()));
        }
    }

    /**
     * Fetch an order by its human-readable reference, e.g. {@code JHC-20260520-0003}.
     * <p>Must be declared BEFORE {@code /{orderId}} so Spring does not attempt to
     * parse the literal segment {@code "ref"} as a {@code Long}.
     */
    @GetMapping("/ref/{customOrderId}")
    public ResponseEntity<?> getOrderByRef(
            @PathVariable String customOrderId,
            Authentication authentication) {
        try {
            return ResponseEntity.ok(
                    orderService.getOrderByRef(customOrderId, currentUserService.requireUser(authentication)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Loading failed: " + e.getMessage()));
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
            rethrowApiException(e);
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

    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders(Authentication authentication) {
        com.example.jhapcham.user.domain.User user = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(orderService.getOrdersForUser(user.getId()));
    }

    @GetMapping("/my-orders/list")
    public ResponseEntity<?> getMyOrdersSimple(Authentication authentication) {
        com.example.jhapcham.user.domain.User user = currentUserService.requireUser(authentication);
        return ResponseEntity.ok(orderService.getOrdersForUserSimple(user.getId()));
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
            rethrowApiException(e);
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
            rethrowApiException(e);
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
            rethrowApiException(e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Cancel failed: " + e.getMessage()));
        }
    }

    @PutMapping("/my-orders/cancel/{orderId}")
    public ResponseEntity<?> myOrdersCancel(
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            com.example.jhapcham.user.domain.User user = currentUserService.requireUser(authentication);
            return ResponseEntity.ok(orderService.customerCancelOrder(orderId, user.getId()));
        } catch (RuntimeException e) {
            rethrowApiException(e);
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
            rethrowApiException(e);
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
            rethrowApiException(e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Processing failed: " + e.getMessage()));
        }
    }

    @PutMapping("/seller/{sellerId}/cancel/{orderId}")
    public ResponseEntity<?> sellerCancel(
            @PathVariable Long sellerId,
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        try {
            currentUserService.requireSellerSelfOrAdmin(currentUserService.requireUser(authentication), sellerId);
            return ResponseEntity.ok(orderService.sellerCancelOrder(orderId, sellerId, reason));
        } catch (RuntimeException e) {
            rethrowApiException(e);
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
            rethrowApiException(e);
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
            rethrowApiException(e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("OTP resend failed: " + e.getMessage()));
        }
    }

    private void rethrowApiException(RuntimeException e) {
        if (e instanceof AuthorizationException || e instanceof OrderStateConflictException || e instanceof IllegalStateException) {
            throw e;
        }
    }
}
