package com.example.jhapcham.payment.application;


import com.example.jhapcham.payment.application.*;
import com.example.jhapcham.payment.domain.*;
import com.example.jhapcham.payment.dto.*;
import com.example.jhapcham.payment.persistence.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.payment.esewa")
public class EsewaProperties {
    private boolean enabled = true;
    private String productCode;
    private String secretKey;
    private String paymentUrl;
    private String statusUrl;
    private String frontendBaseUrl;
}
