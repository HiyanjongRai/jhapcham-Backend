package com.example.jhapcham.Checkout;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/start")
    public ResponseEntity<CheckoutSession> startCheckout(
            @RequestParam Long userId,
            @RequestParam String fullAddress,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam Boolean insideValley,
            @RequestParam PaymentMethod paymentMethod
    ) {
        return ResponseEntity.ok(
                checkoutService.startCheckout(
                        userId,
                        fullAddress,
                        lat,
                        lng,
                        insideValley,
                        paymentMethod
                )
        );
    }

    @PostMapping("/pay/{checkoutId}")
    public ResponseEntity<CheckoutSession> markPaymentSuccess(
            @PathVariable Long checkoutId
    ) {
        return ResponseEntity.ok(
                checkoutService.markPaid(checkoutId)
        );
    }

    @GetMapping("/{checkoutId}")
    public ResponseEntity<CheckoutSession> getCheckout(
            @PathVariable Long checkoutId
    ) {
        return ResponseEntity.ok(
                checkoutService.getById(checkoutId)
        );
    }


}