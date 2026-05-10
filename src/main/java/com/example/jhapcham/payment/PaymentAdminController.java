package com.example.jhapcham.payment;

import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<?> listPayments(Authentication authentication) {
        try {
            currentUserService.requireAdmin(currentUserService.requireUser(authentication));
            List<Payment> payments = paymentRepository.findAll();
            List<PaymentAdminDTO> dtos = payments.stream().map(this::toAdminDTO).toList();
            return ResponseEntity.ok(dtos);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to load payments"));
        }
    }

    @GetMapping("/{paymentId}/events")
    public ResponseEntity<?> getEvents(@PathVariable Long paymentId, Authentication authentication) {
        try {
            currentUserService.requireAdmin(currentUserService.requireUser(authentication));
            return ResponseEntity.ok(paymentEventRepository.findByPayment_IdOrderByCreatedAtDesc(paymentId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to load payment events"));
        }
    }

    private PaymentAdminDTO toAdminDTO(Payment p) {
        Order o = p.getOrder();
        return PaymentAdminDTO.builder()
                .paymentId(p.getId())
                .orderId(o != null ? o.getId() : null)
                .customerEmail(o != null ? o.getCustomerEmail() : null)
                .customerName(o != null ? o.getCustomerName() : null)
                .orderStatus(o != null ? String.valueOf(o.getStatus()) : null)
                .method(String.valueOf(p.getMethod()))
                .state(String.valueOf(p.getState()))
                .amount(p.getAmount())
                .transactionUuid(p.getTransactionUuid())
                .providerReferenceId(p.getProviderReferenceId())
                .initiatedAt(p.getInitiatedAt())
                .completedAt(p.getCompletedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
