package com.example.jhapcham.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class PaymentFrontendProperties {
    @Value("${app.payment.frontend-base-url:http://localhost:3000}")
    private String baseUrl;
}
