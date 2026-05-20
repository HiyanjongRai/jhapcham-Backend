package com.example.jhapcham.refund;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.refund.gateway")
public class RefundGatewayProperties {
    private GatewayEndpoint esewa = new GatewayEndpoint();
    private GatewayEndpoint khalti = new GatewayEndpoint();

    @Getter
    @Setter
    public static class GatewayEndpoint {
        private boolean enabled;
        private String refundUrl;
        private String apiKey;
    }
}
