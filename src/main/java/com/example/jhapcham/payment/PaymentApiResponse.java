package com.example.jhapcham.payment;

import com.example.jhapcham.order.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentApiResponse {
    private boolean success;
    private String message;

    @JsonProperty("error_code")
    private String errorCode;

    private Long orderId;
    private PaymentStatus paymentStatus;
    private PaymentState paymentState;
    private String transactionUuid;
    private String refId;
    private Map<String, Object> data;
}
