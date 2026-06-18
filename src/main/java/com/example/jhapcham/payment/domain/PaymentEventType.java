package com.example.jhapcham.payment.domain;


import com.example.jhapcham.payment.application.*;
import com.example.jhapcham.payment.domain.*;
import com.example.jhapcham.payment.dto.*;
import com.example.jhapcham.payment.persistence.*;
public enum PaymentEventType {
    INITIATE_REQUEST,
    INITIATE_RESPONSE,
    CALLBACK_RECEIVED,
    VERIFICATION_FAILED,
    VERIFICATION_SUCCEEDED,
    PAYMENT_FAILURE,
    DUPLICATE_TRANSACTION,
    STATUS_CHECK_REQUEST,
    STATUS_CHECK_RESPONSE,
    STATE_CHANGED
}
