package com.example.jhapcham.payment.api;


import com.example.jhapcham.payment.application.*;
import com.example.jhapcham.payment.domain.*;
import com.example.jhapcham.payment.dto.*;
import com.example.jhapcham.payment.persistence.*;
import com.example.jhapcham.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment/khalti")
@RequiredArgsConstructor
public class KhaltiPaymentController {

    private final KhaltiPaymentService khaltiPaymentService;
    private final PaymentFrontendProperties paymentFrontendProperties;
    private final CurrentUserService currentUserService;

    @PostMapping("/initiate")
    public ResponseEntity<?> initiate(@RequestBody Map<String, Object> request, Authentication authentication) {
        List<Long> orderIds = extractOrderIds(request.get("orderIds"));
        BigDecimal amount = new BigDecimal(String.valueOf(request.get("amount")));
        String purchaseOrderId = String.valueOf(request.get("purchaseOrderId"));
        return ResponseEntity.ok(khaltiPaymentService.initiateOrderPayment(
                orderIds, amount, purchaseOrderId, currentUserService.requireUser(authentication)));
    }

    @PostMapping("/commission/initiate")
    public ResponseEntity<?> initiateCommission(@RequestBody Map<String, Object> request, Authentication authentication) {
        List<Long> orderIds = extractOrderIds(request.get("orderIds"));
        BigDecimal amount = new BigDecimal(String.valueOf(request.get("amount")));
        String purchaseOrderId = String.valueOf(request.get("purchaseOrderId"));
        return ResponseEntity.ok(khaltiPaymentService.initiateCommissionPayment(
                orderIds, amount, purchaseOrderId, currentUserService.requireUser(authentication)));
    }

    @GetMapping("/order/success")
    public RedirectView orderSuccess(@RequestParam String pidx,
                                     @RequestParam(name = "purchase_order_id") String purchaseOrderId,
                                     @RequestParam(required = false) String status) {
        try {
            khaltiPaymentService.verifyOrderCallback(pidx, purchaseOrderId, status);
            return new RedirectView(frontendBaseUrl() + "/payment/success?gateway=KHALTI&pidx=" + pidx + "&purchaseOrderId=" + purchaseOrderId);
        } catch (Exception e) {
            log.warn("Khalti order verification failed", e);
            return new RedirectView(frontendBaseUrl() + "/payment/failure?gateway=KHALTI");
        }
    }

    @GetMapping("/commission/success")
    public RedirectView commissionSuccess(@RequestParam String pidx,
                                          @RequestParam(name = "purchase_order_id") String purchaseOrderId,
                                          @RequestParam(required = false) String status) {
        try {
            khaltiPaymentService.verifyCommissionCallback(pidx, purchaseOrderId, status);
            return new RedirectView(frontendBaseUrl() + "/seller/commission?khaltiCommission=success");
        } catch (Exception e) {
            log.warn("Khalti commission verification failed", e);
            return new RedirectView(frontendBaseUrl() + "/seller/commission?khaltiCommission=failed");
        }
    }

    private List<Long> extractOrderIds(Object raw) {
        if (raw instanceof List<?> list) {
            return list.stream().map(value -> Long.parseLong(String.valueOf(value))).toList();
        }
        if (raw instanceof String text && !text.isBlank()) {
            return java.util.Arrays.stream(text.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(Long::parseLong)
                    .toList();
        }
        throw new com.example.jhapcham.Error.BusinessValidationException("Order IDs are required for Khalti payment");
    }

    private String frontendBaseUrl() {
        return paymentFrontendProperties.getBaseUrl();
    }
}
