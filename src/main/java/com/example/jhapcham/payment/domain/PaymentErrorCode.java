package com.example.jhapcham.payment.domain;


import com.example.jhapcham.payment.application.*;
import com.example.jhapcham.payment.domain.*;
import com.example.jhapcham.payment.dto.*;
import com.example.jhapcham.payment.persistence.*;
public enum PaymentErrorCode {
    PAYMENT_CANCELLED,
    @Deprecated
    ESEWA_LOGIN_FAILED,
    INSUFFICIENT_BALANCE,
    INVALID_SIGNATURE,
    PAYMENT_VERIFICATION_FAILED,
    PAYMENT_PENDING_VERIFICATION,
    DUPLICATE_TRANSACTION,
    PAYMENT_NOT_RETRYABLE,
    ORDER_NOT_FOUND,
    PAYMENT_NOT_FOUND,
    SERVER_ERROR
}
