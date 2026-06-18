package com.example.jhapcham.payment.application;


import com.example.jhapcham.payment.application.*;
import com.example.jhapcham.payment.domain.*;
import com.example.jhapcham.payment.dto.*;
import com.example.jhapcham.payment.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.payment.khalti")
public class KhaltiProperties {
    private boolean enabled = true;
    private String secretKey;
    private String publicKey;
    private String baseUrl = "https://dev.khalti.com/api/v2";
    private String websiteUrl = "http://localhost:3000";
    private String backendBaseUrl = "http://localhost:8080";
}
