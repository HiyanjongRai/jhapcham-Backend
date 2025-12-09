//package com.example.jhapcham.order;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/orders")
//@RequiredArgsConstructor
//@CrossOrigin(origins = "*")
//public class OrderViewController {
//
//    private final OrderRepository orderRepository;
//    private final OrderService orderService;
//
//    // View all orders. Admin use
//    @GetMapping("/all")
//    public ResponseEntity<?> getAllOrders() {
//        return ResponseEntity.ok(orderRepository.findAll());
//    }
//
//    // View orders by user
//    @GetMapping("/user/{userId}")
//    public ResponseEntity<?> getUserOrders(@PathVariable Long userId) {
//        return ResponseEntity.ok(
//                orderRepository.findByCustomer_Id(userId)
//        );
//    }
//
//    // View orders by seller
//    @GetMapping("/seller/{sellerId}")
//    public ResponseEntity<?> getSellerOrders(@PathVariable Long sellerId) {
//        return ResponseEntity.ok(
//                orderRepository.findSellerOrders(sellerId)
//        );
//    }
//
//    // View single order by orderId
//    @GetMapping("/{orderId}")
//    public ResponseEntity<?> getOneOrder(@PathVariable Long orderId) {
//        return ResponseEntity.ok(
//                orderRepository.findById(orderId)
//                        .orElseThrow(() -> new RuntimeException("Order not found"))
//        );
//    }
//
//    // Create order from checkout
//    @PostMapping("/from-checkout/{checkoutId}")
//    public ResponseEntity<?> createOrderFromCheckout(@PathVariable Long checkoutId) {
//        return ResponseEntity.ok(
//                orderService.createOrderFromCheckout(checkoutId)
//        );
//    }
//
//
//}