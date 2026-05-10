package com.example.jhapcham.payment;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PaymentWorkflowException extends RuntimeException {
    private final PaymentErrorCode errorCode;
    private final HttpStatus status;

    public PaymentWorkflowException(PaymentErrorCode errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
