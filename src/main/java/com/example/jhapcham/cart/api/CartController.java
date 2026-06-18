package com.example.jhapcham.cart.api;


import com.example.jhapcham.cart.application.*;
import com.example.jhapcham.cart.domain.*;
import com.example.jhapcham.cart.dto.*;
import com.example.jhapcham.cart.persistence.*;
import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @PostMapping("/{userId}/add/{productId}")
    public ResponseEntity<?> addToCart(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestBody AddToCartRequestDTO dto,
            Authentication authentication) {
        try {
            currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
            CartResponseDTO cart = cartService.addToCart(userId, productId, dto);

            return ResponseEntity.ok(cart);

        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(new ErrorResponse(e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity
                    .status(500)
                    .body(new ErrorResponse("Unable to add item to cart"));
        }

    }

    @PutMapping("/{userId}/update/{cartItemId}")
    public ResponseEntity<?> updateQuantity(
            @PathVariable Long userId,
            @PathVariable Long cartItemId,
            @RequestParam Integer qty,
            Authentication authentication) {
        try {
            currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
            CartResponseDTO cart = cartService.updateQuantity(userId, cartItemId, qty);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to update cart item"));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Long userId, Authentication authentication) {
        try {
            currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
            CartResponseDTO cart = cartService.getCart(userId);
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch cart"));
        }
    }

}
