package com.example.jhapcham.refund;

import com.example.jhapcham.order.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundGatewayService {

    private final RefundGatewayProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GatewayRefundResult requestRefund(RefundGateway gateway, RefundRequest request, RefundTransaction transaction) {
        if (gateway == RefundGateway.COD_MANUAL || gateway == RefundGateway.MANUAL) {
            return GatewayRefundResult.builder()
                    .accepted(true)
                    .succeeded(false)
                    .providerRefundReference(transaction.getIdempotencyKey())
                    .message("Manual reconciliation required before marking refund as paid")
                    .build();
        }

        RefundGatewayProperties.GatewayEndpoint endpoint = endpointFor(gateway);
        if (endpoint == null || !endpoint.isEnabled() || endpoint.getRefundUrl() == null || endpoint.getRefundUrl().isBlank()) {
            return GatewayRefundResult.builder()
                    .accepted(false)
                    .succeeded(false)
                    .message(gateway + " refund endpoint is not configured")
                    .build();
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            Order order = request.getOrder();
            payload.put("refundRequestId", request.getId());
            payload.put("orderId", order.getId());
            payload.put("paymentReference", order.getPaymentReference());
            payload.put("amount", request.getTotalRefund());
            payload.put("idempotencyKey", transaction.getIdempotencyKey());
            payload.put("reason", request.getReason().name());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (endpoint.getApiKey() != null && !endpoint.getApiKey().isBlank()) {
                headers.setBearerAuth(endpoint.getApiKey());
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint.getRefundUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    String.class);

            String body = response.getBody();
            boolean success = response.getStatusCode().is2xxSuccessful()
                    && body != null
                    && body.toLowerCase().contains("success");
            String providerReference = extractProviderReference(body);
            return GatewayRefundResult.builder()
                    .accepted(response.getStatusCode().is2xxSuccessful())
                    .succeeded(success)
                    .providerRefundReference(providerReference != null ? providerReference : transaction.getIdempotencyKey())
                    .rawResponse(body)
                    .message(response.getStatusCode().toString())
                    .build();
        } catch (Exception ex) {
            log.error("Refund gateway {} request failed for refund {}", gateway, request.getId(), ex);
            return GatewayRefundResult.builder()
                    .accepted(false)
                    .succeeded(false)
                    .message(ex.getMessage())
                    .build();
        }
    }

    public String buildRequestPayload(RefundRequest request, RefundGateway gateway, String idempotencyKey) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("gateway", gateway);
            payload.put("refundRequestId", request.getId());
            payload.put("orderId", request.getOrder().getId());
            payload.put("amount", request.getTotalRefund());
            payload.put("idempotencyKey", idempotencyKey);
            payload.put("reason", request.getReason());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    private RefundGatewayProperties.GatewayEndpoint endpointFor(RefundGateway gateway) {
        return switch (gateway) {
            case ESEWA -> properties.getEsewa();
            case KHALTI -> properties.getKhalti();
            default -> null;
        };
    }

    private String extractProviderReference(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            Map<?, ?> parsed = objectMapper.readValue(body, Map.class);
            Object ref = parsed.get("refundId");
            if (ref == null) ref = parsed.get("refund_id");
            if (ref == null) ref = parsed.get("referenceId");
            if (ref == null) ref = parsed.get("reference_id");
            return ref != null ? String.valueOf(ref) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Data
    @Builder
    public static class GatewayRefundResult {
        private boolean accepted;
        private boolean succeeded;
        private String providerRefundReference;
        private String rawResponse;
        private String message;
    }
}
