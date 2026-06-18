package com.example.jhapcham.delivery.api;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipment")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final com.example.jhapcham.order.persistence.OrderRepository orderRepository;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @PostMapping("/create/{orderId}")
    public ResponseEntity<?> createShipment(@PathVariable Long orderId, Authentication authentication) {
        try {
            assertCanCreateShipment(orderId, authentication);
            return ResponseEntity.ok(shipmentService.createShipment(orderId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getShipment(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(shipmentService.getShipment(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    private void assertCanCreateShipment(Long orderId, Authentication authentication) {
        var actor = currentUserService.requireUser(authentication);
        if (actor.getRole() == com.example.jhapcham.user.domain.Role.ADMIN) {
            return;
        }
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new com.example.jhapcham.Error.ResourceNotFoundException("Order not found"));
        boolean ownsAllItems = actor.getRole() == com.example.jhapcham.user.domain.Role.SELLER
                && order.getItems().stream().allMatch(item -> item.getProduct() != null
                && item.getProduct().getSellerProfile() != null
                && item.getProduct().getSellerProfile().getUser() != null
                && item.getProduct().getSellerProfile().getUser().getId().equals(actor.getId()));
        if (!ownsAllItems) {
            throw new com.example.jhapcham.Error.AuthorizationException("You do not have permission to create this shipment");
        }
    }
}
